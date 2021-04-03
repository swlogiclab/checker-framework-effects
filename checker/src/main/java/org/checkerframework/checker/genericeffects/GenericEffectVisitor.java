package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import java.lang.annotation.Annotation;
import java.util.Deque;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.genericeffects.qual.Impossible;
import org.checkerframework.checker.genericeffects.qual.ThrownEffect;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreeUtils;

/**
 * GenericEffectVisitor is a base class for effect systems, including sequential effect systems.
 * 
 * The general idea is for checking the effect of a method to proceed by initializing an accumulator
 * to the unit effect for the particular system (from {@link GenericEffectLattice.getUnitEffect()}),
 * then recursively traverse the AST to accumulate the overall effect of various subtrees. Each
 * visit method should leave the accumulator holding the effect of (only) the AST node visited.
 * 
 * <p>Because methods can actually nest (if a method includes an anonymous inner class with a method 
 * definition), we actually keep a <emph>stack</emph> of accumulators. The top element of the stack is
 * the accumulator for the current context.<p>
 * 
 * <p>The stack depth changes whenever a new AST node is visited.  Upon entry to any visit method,
 * the top-most element of the stack should contain the effect of the method so far (i.e., the
 * preceeding context effect), which will aid (with further extension) in precise error reporting
 * for sequential effect systems.  Upon exit, the method should leave the stack the same depth
 * as upon entry..... HMM, this is getting messy, there's a tension here between updating the
 * stack consistently so subexpressions can give good error reporting, and keeping things separate
 * so non-sequential composition (e.g., conditionals/switch) can compute the right upper bound to leave for the next thiing.... or maybe snapshotting and exploring all paths is enough due to distrib laws? Except for loops... and then exceptions, no we need modular tracking to assign effects to individual subexpressions.  So maybe we need a stack of stacks, error reporting from accumulating across top-most stack :-p</p>
 * 
 * TODO: Add methods to the GenericEffectChecker to configure default upper bounds on static and
 * instance field initializers (static runs anywhere, field runs with <emph>every</emph> ctor).
 */
public class GenericEffectVisitor extends BaseTypeVisitor<GenericEffectTypeFactory> {

    protected final boolean debugSpew;
    private GenericEffectLattice genericEffect;
    private GenericEffectExtension extension;

    // effStack and currentMethods should always be the same size.
    protected final Deque<ContextEffect> effStack;
    protected final Deque<MethodTree> currentMethods;

    // fields for compiler arguments
    boolean ignoringEffects;
    boolean ignoringWarnings;
    boolean ignoringErrors;

    /**
     * Constructor that takes passes the checker to the superclass and takes in a
     * GenericEffectExtension object. The effect stack for methods and variables are set up in the
     * constructor.
     *
     * @param checker The checker that allows the Casting Effects Checker to function.
     * @param ext An GenericEffectExtension object that provides the developer with more functions
     *     dealing with specific tree nodes.
     */
    @SuppressWarnings("JdkObsolete")
    public GenericEffectVisitor(BaseTypeChecker checker, GenericEffectExtension ext) {
        super(checker);
        assert (checker instanceof GenericEffectChecker);
        debugSpew = checker.getLintOption("debugSpew", false);

        /* ErrorProne JdkObsolete warnings are suppressed here because we must use a deque/stack implementation that permits null.
         * Without supressing this warning, ErrorProne complains we should be using ArrayDeque, which rejects null elements. */
        effStack = new LinkedList<ContextEffect>();
        currentMethods = new LinkedList<MethodTree>();

        extension = ext;

        ignoringEffects = checker.getOption("ignoreEffects") != null;
        ignoringWarnings = checker.getOption("ignoreWarnings") != null;
        ignoringErrors = checker.getOption("ignoreErrors") != null;

        genericEffect = ((GenericEffectChecker) checker).getEffectLattice();
    }

    /**
     * Method to instantiate the factory class for the checker.
     *
     * @return The type factory of the checker.
     */
    @Override
    protected GenericEffectTypeFactory createTypeFactory() {
        return new GenericEffectTypeFactory(checker, debugSpew);
    }

    /**
     * TODO: Please document the use off this with respect to the generic effect checker better.
     * Note: The GuiEffectChecker uses a similar setup and provides more documentation.
     *
     * @param node Class declaration to process
     */
    @Override
    public void processClassTree(ClassTree node) {
        // Fix up context for static initializers of new class
        currentMethods.addFirst(null);
        effStack.addFirst(new ContextEffect(genericEffect));
        super.processClassTree(node);
        currentMethods.removeFirst();
        effStack.removeFirst();
    }

    /**
     * Method that visits method tree nodes and adds their effects to the stacks set up in the
     * constructor.
     *
     * @param node The method tree node that was encountered during checking.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitMethod(MethodTree node, Void p) {

        ExecutableElement methElt = TreeUtils.elementFromDeclaration(node);
        if (debugSpew) {
            System.err.println("\nVisiting method " + methElt);
        }

        assert (methElt != null);

        // Override check
        atypeFactory.checkEffectOverride(
                (TypeElement) methElt.getEnclosingElement(), methElt, true, node);

        Map<Class<? extends Exception>,Class<? extends Annotation>> excBehaviors = new HashMap<>();
        // Check that any @ThrownEffect uses are valid
        for (AnnotationMirror thrown : atypeFactory.getDeclAnnotations(methElt)) {
            if (thrown.getClass() == ThrownEffect.class) {
                ThrownEffect thrownEff = (ThrownEffect)thrown;
                // TODO: require the effect be a checked exception (i.e., not subtype of RuntimeException)
                Class<? extends Annotation> prev = excBehaviors.put(thrownEff.exception(), thrownEff.behavior());
                if (prev != null) {
                    checker.reportError(node, "duplicate.annotation.thrown", thrownEff.exception(), thrownEff.behavior(), prev);
                }
            }
        }

        // Initialize method stack
        currentMethods.addFirst(node);
        effStack.addFirst(new ContextEffect(genericEffect));

        if (debugSpew) {
            System.err.println(
                    "Pushing " + effStack.peekFirst() + " onto the stack when checking " + methElt);
        }

        Void ret = super.visitMethod(node, p);
      
        Class<? extends Annotation> targetEffect = effStack.peek().currentPathEffect();
        Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(methElt);
        if (isInvalid(targetEffect, callerEffect))
            checkError(node, targetEffect, callerEffect, extension.reportError(node));
        else if (extension.reportWarning(node) != null)
            checkWarning(node, targetEffect, callerEffect, extension.reportWarning(node));
        
        currentMethods.removeFirst();
        effStack.removeFirst();
        return ret;
    }

    /**
     * Method that can be used in a visitor method to see if a node is enclosed by a method.
     *
     * @return A boolean representing whether the node is enclosed by a method (true) or not
     *     (false).
     */
    private boolean hasEnclosingMethod() {
        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        return callerTree != null;
    }

    /**
     * Method to check is a target effect and a caller effect are invalid according to the lattice.
     * The method also checks which effects are to be ignored.
     *
     * @param targetEffect Target effect of node.
     * @param callerEffect Caller effect of node.
     * @return Boolean value representing whether the effects are invalid (true) or not (false)
     */
    private boolean isInvalid(
            Class<? extends Annotation> targetEffect, Class<? extends Annotation> callerEffect) {
        if (ignoringEffects)
            targetEffect =
                    extension.checkIgnoredEffects(checker.getOption("ignoreEffects"), targetEffect);
        if (!genericEffect.LE(targetEffect, callerEffect)) return true;
        return false;
    }

    /**
     * Method that reports an error as specified by given parameters. The method also checks which
     * errors are to be ignored.
     *
     * @param node Node for which error should be reported.
     * @param targetEffect Target effect of node.
     * @param callerEffect Caller effect of node.
     * @param failureMsg Error message to be reported.
     */
    private void checkError(
            Tree node,
            Class<? extends Annotation> targetEffect,
            Class<? extends Annotation> callerEffect,
            @CompilerMessageKey String failureMsg) {
        if (!ignoringErrors) checker.reportError(node, failureMsg, targetEffect, callerEffect);
        else if (ignoringErrors
                && !extension.isIgnored(checker.getOption("ignoreErrors"), failureMsg))
            checker.reportError(node, failureMsg, targetEffect, callerEffect);
    }

    private void checkResidual(
            Tree node) {
        if (!ignoringErrors) {
            Class<? extends Annotation> pathEffect = effStack.peek().currentPathEffect();
            if (pathEffect == Impossible.class) {
                return; // In an enclosing context of a path that always throws/returns
            }
            Class<? extends Annotation> methodEffect = atypeFactory.getDeclaredEffect(TreeUtils.elementFromDeclaration(currentMethods.peek()));
            if (genericEffect.residual(pathEffect, methodEffect) == null) {
                checker.reportError(node, "undefined.residual", pathEffect, methodEffect);
            }
        }
    }

    /**
     * Method that reports a warning as specified by the given parameters. The method also checks
     * which warnings are to be ignored.
     *
     * @param node Node for which warning should be reported.
     * @param targetEffect Target effect of node.
     * @param callerEffect Caller effect of node.
     * @param warningMsg Warning message to be reported.
     */
    private void checkWarning(
            Tree node,
            Class<? extends Annotation> targetEffect,
            Class<? extends Annotation> callerEffect,
            @CompilerMessageKey String warningMsg) {
        if (!ignoringWarnings) checker.reportWarning(node, warningMsg, targetEffect, callerEffect);
        else if (ignoringWarnings
                && !extension.isIgnored(checker.getOption("ignoreWarnings"), warningMsg))
            checker.reportWarning(node, warningMsg, targetEffect, callerEffect);
    }

    /**
     * Method that is used by visitor methods to get the effect of a method that a node is within.
     *
     * @return Effect of a method that a node is within.
     */
    private Class<? extends Annotation> getMethodCallerEffect() {
        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
        return atypeFactory.getDeclaredEffect(callerElt);
    }

    /**
     * Method that is used in a visitor method to get the default effect a class that a node is
     * within.
     * 
     * TODO: Must split between static vs. instance field initializers
     *
     * @return The default effect of a class that a node is within.
     */
    private Class<? extends Annotation> getDefaultClassEffect() {
        ClassTree clsTree = TreeUtils.enclosingClass(getCurrentPath());
        Element clsElt = TreeUtils.elementFromDeclaration(clsTree);
        return atypeFactory.getDefaultEffect(clsElt);
    }

    /**
     * Method that visits all the method invocation tree nodes and raises failures/warnings for
     * unsafe method invocations.
     *
     * @param node Method invocation tree node that is found during checking.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        // TODO extension checks
        // Set marker for scoping current 
        effStack.peek().mark();
	    scan(node.getMethodSelect(), p);
        for(Tree args : node.getArguments()){
            scan(args, p);
        }
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    /**
     * Method to check if the constructor call is made from a valid context.
     * 
     * TODO: Fix for static vs. instance initializers
     *
     * @param node New class tree node that is found during checking.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        // TODO extension checks
        // Set marker for scoping current 
        effStack.peek().mark();
        for(Tree args : node.getArguments()){
            scan(args, p);
        }
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    /**
     * The methods below this comment follow the same format. Each method is a different visit
     * method for a different kind of tree node. Using the extensions class the developer can
     * activate specific visitor methods depending on what they want to check.
     *
     * <p>The methods work by first checking if the node being checked is enclosed by a method. If
     * it is then the method obtains the effect of the node and checks it against the method's
     * effect. If the node is not enclosed by a method, then it checks at the variable level against
     * the class annotation.
     *
     * @param node Specific tree node that is to be checked.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        // TODO extension checks
        // Set marker for scoping current 
        effStack.peek().mark();
	    scan(node.getExpression(), p);
	    scan(node.getIndex(), p);
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree node, Void p) {
        if (extension.doesArrayTypeCheck()) {
            effStack.peek().pushEffect(extension.checkArrayType(node), node);
            checkResidual(node);
        }
        return super.visitArrayType(node, p);
    }

    @Override
    public Void visitAssert(AssertTree node, Void p) {
        // TODO extension checks
        // Assertions may or may not execute, so ensure either possibility is acceptable
        effStack.peek().mark();
	    scan(node.getCondition(), p);
        Class<? extends Annotation> condEff = effStack.peek().squashMark(node);
        Class<? extends Annotation> joinWithUnit = genericEffect.LUB(genericEffect.unit(), condEff);
        if (joinWithUnit == null) {
            checker.reportError(node, "undefined.join.assertion", condEff);
        }
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        // TODO extension checks
        // Set marker for scoping current 
        effStack.peek().mark();
	    scan(node.getVariable(), p);
	    scan(node.getExpression(), p);
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        // TODO extension checks
        // Set marker for scoping current 
        effStack.peek().mark();
	    scan(node.getLeftOperand(), p);
	    scan(node.getRightOperand(), p);
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitBreak(BreakTree node, Void p) {
        if (extension.doesBreakCheck()) {
            effStack.peek().pushEffect(extension.checkBreak(node), node);
            checkResidual(node);
        }
        return super.visitBreak(node, p);
    }

    @Override
    public Void visitCase(CaseTree node, Void p) {
        // TODO: need extra plumbing to be sound w.r.t. fallthrough

        effStack.peek().mark();
        scan(node.getExpression(), p);
        scan(node.getStatements(), p);
        effStack.peek().squashMark(node);
        // TODO: incorporate extension behavior
        checkResidual(node);
;       return p;
    }

    @Override
    public Void visitCatch(CatchTree node, Void p) {
        // TODO: implement, with extension
        throw new UnsupportedOperationException("Exceptions are not yet supported");
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        // TODO extension checks
        // Set marker for scoping current 
        effStack.peek().mark();
	    scan(node.getVariable(), p);
	    scan(node.getExpression(), p);
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitContinue(ContinueTree node, Void p) {
        // TODO: implement, with extension. Non-trivial due to continue-to-label
        if (node.getLabel() == null) {
            if (extension.doesContinueCheck()) {
                effStack.peek().pushEffect(extension.checkContinue(node), node);
                effStack.peek().squashMark(node);
                checkResidual(node);
            }
        } else {
            throw new UnsupportedOperationException("");
        }
        return p;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        // Set mark for full expression
        effStack.peek().mark();

        scan(node.getStatement(), p);
        Class<? extends Annotation> bodyEff = effStack.peek().latestEffect();
        scan(node.getCondition(), p);
        Class<? extends Annotation> condEff = effStack.peek().latestEffect();

        // Here we DO NOT simply squash, because we must invoke iteration
        LinkedList<Class<? extends Annotation>> pieces = effStack.peek().rewindToMark();
        assert (pieces.get(0) == bodyEff);
        assert (pieces.get(1) == condEff);

        Class<? extends Annotation> repeff = genericEffect.iter(genericEffect.seq(bodyEff, condEff));
        if (repeff == null) {
            checker.reportError(node, "undefined.repetition.twopart", bodyEff, condEff);
            // Pretend we ran the loop and condition once each 
            boolean success = effStack.peek().pushEffect(genericEffect.seq(bodyEff, condEff), node);
            assert success; // TODO fix
        } else {
            // Valid iteration
            Class<? extends Annotation> eff = genericEffect.seq(condEff, repeff);
            boolean success = effStack.peek().pushEffect(eff, node);
            assert success; // TODO fix
        }
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        // TODO implement: need to handle effects of implicit calls to the iterator methods
        throw new UnsupportedOperationException("foreach support does not exist yet");
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void p) {
        // TODO: extension
        // Set mark for full node 
        effStack.peek().mark();

        // Scan the initializer statements (implicitly, in order)
        scan(node.getInitializer(), p);
        Class<? extends Annotation> initEff = effStack.peek().latestEffect();
        scan(node.getCondition(), p);
        Class<? extends Annotation> condEff = effStack.peek().latestEffect();
        scan(node.getStatement(), p);
        Class<? extends Annotation> bodyEff = effStack.peek().latestEffect();
        // mark for updates, since there may be multiple
        effStack.peek().mark();
        scan(node.getUpdate(), p);
        Class<? extends Annotation> updateEff = effStack.peek().squashMark(null);

        // If we're reached here, it's possible to run the initializers, cond, body, update in that order
        // We care about iterating body-update-cond, though

        // Here we DO NOT simply squash, because we must invoke iteration
        LinkedList<Class<? extends Annotation>> pieces = effStack.peek().rewindToMark();
        assert (pieces.get(0) == initEff);
        assert (pieces.get(1) == condEff);
        assert (pieces.get(2) == bodyEff);
        assert (pieces.get(3) == updateEff);

        Class<? extends Annotation> repeff = genericEffect.iter(genericEffect.seq(genericEffect.seq(bodyEff, updateEff), condEff));
        if (repeff == null) {
            checker.reportError(node, "undefined.repetition.threepart", bodyEff, updateEff, condEff);
            // Pretend we ran the loop exactly once: init, condition, loop, update, and condition again
            effStack.peek().pushEffect(genericEffect.seq(condEff, genericEffect.seq(bodyEff, condEff)), node);
        } else {
            // Valid iteration
            Class<? extends Annotation> eff = genericEffect.seq(genericEffect.seq(initEff, condEff), repeff);
            effStack.peek().pushEffect(eff, node);
        }
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        return checkConditional(node, node.getCondition(), node.getThenStatement(), node.getElseStatement(), p);
    }
    
    protected Void checkConditional(Tree node, ExpressionTree condTree, Tree thenTree, Tree elseTree, Void p) {

        // One mark for the whole node, nested mark for each branch.
        effStack.peek().mark();
	    scan(condTree, p); 
        effStack.peek().mark();
        scan(thenTree, p);
        LinkedList<Class<? extends Annotation>> thenEffs = effStack.peek().rewindToMark();
        assert (thenEffs.size() == 1);
        Class<? extends Annotation> thenEff = thenEffs.get(0);
        Class<? extends Annotation> elseEff = genericEffect.unit();
        if (elseTree != null) {
            effStack.peek().mark();
            scan(elseTree, p);
            LinkedList<Class<? extends Annotation>> elseEffs = effStack.peek().rewindToMark();
            assert (elseEffs.size() == 1);
            elseEff = elseEffs.get(0);
        }
        // stack still has the condition effect on it, but no branch effects
        LinkedList<Class<? extends Annotation>> condEffs = effStack.peek().rewindToMark();
        assert (condEffs.size() == 1);
        Class<? extends Annotation> condEff = condEffs.get(0);

        if (thenEff == Impossible.class && elseEff == Impossible.class) {
            // Both branches return and/or throw, so regular paths through this term
            // do not return normally
            effStack.peek().markImpossible(node);
        } else if (thenEff == Impossible.class) {
            effStack.peek().pushEffect(genericEffect.seq(condEff,elseEff), node);
        } else if (elseEff == Impossible.class) {
            effStack.peek().pushEffect(genericEffect.seq(condEff,thenEff), node);
        } else {
            // Both branches possible: the common case
            Class<? extends Annotation> lub = genericEffect.LUB(thenEff, elseEff);
            if (lub == null) {
                if (elseTree == null) {
                    checker.reportError(node, "undefined.join.unaryif", thenEff, elseEff);
                } else {
                    checker.reportError(node, "undefined.join", thenEff, elseEff);
                }
            }

            // This seq will always succeed (with valid EQs) since the seqs worked per-branch, and we have distributivity
            effStack.peek().pushEffect(genericEffect.seq(condEff, lub), node);

            // TODO: Figure out when we do/don't want multiple errors issued. Clearly want multiple for cases like traditional LUB systems, but sometimes may want to stop early for truly sequential EQs
            checkResidual(node);
        }
        return p;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        // TODO extension checks
        return checkConditional(node, node.getCondition(), node.getTrueExpression(), node.getFalseExpression(), p);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {
        effStack.peek().mark();
        scan(node.getExpression(), p);
        if (extension.doesInstanceOfCheck()) {
            effStack.peek().pushEffect(extension.checkInstanceOf(node), node);
        }
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitIntersectionType(IntersectionTypeTree node, Void p) {
        if (extension.doesIntersectionTypeCheck()) {
            effStack.peek().pushEffect(extension.checkIntersectionType(node), node);
            checkResidual(node);
        }
        return p;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree node, Void p) {
        // TODO extension
        effStack.peek().mark();
        scan(node.getStatement(), p);
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void p) {
        // TODO extension
        // This has effect unit, shouldn't cause an error unless the extension is in use
        return p;
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        // TODO extension
        // Note: We don't iterate even if there is a single initializer for all array cells, because the expression is evaluated only once, and the value is duplicated
        effStack.peek().mark();
        for (ExpressionTree init : node.getInitializers()) {
            scan(init, p);
        }
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree node, Void p) {
        // TODO extension
        return p;
    }

    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        // TODO extension
        // TODO: Need to handle early returns!!! This currently only handles return from tail position. Exceptions I think are reasonable to punt on for now, but early returns are not
        // TODO: Perhaps I can just keep a stack of "early return effects", which I can handle by just adding the effect so far to that stack? Oh, actually, don't even need that. There's no residual check here, just a LE check!
        effStack.peek().mark();
        scan(node.getExpression(), p);
        effStack.peek().squashMark(node);
        // TODO: is looking at the top of the stack faster than getMethodCallerEffect ?
        if (!genericEffect.LE(effStack.peek().currentPathEffect(), getMethodCallerEffect())) {
            checker.reportError(node, "invalid.return", effStack.peek().currentPathEffect(), getMethodCallerEffect());
        }
        // TODO: Real question is what state I leave the stack in when returning. This will be hit always at the end of a sequence of statements, but sometimes those will be nested inside a conditional or loop.... and want the callers to know not to consider this path --- poison value that can be inspected e.g. in conditional cases, which already save&restore?
        // TODO: Maybe the "set of behaviors" collection is the right way to handle exceptions, as long as I track which exception leads to what... but then I need to handle methods that return effects.... so I need a meta-annotation @ThrowsEffect(Class<?>, Class<? extends Annotation>)!
        effStack.peek().markImpossible(node);
        return p;
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        // TODO extension
        effStack.peek().mark();
        scan(node.getExpression(), p);
        // This is tricky: *assuming no fall-through*, we'll execute some number of case expressions, followed by some (one) body, and possibly a default case.
        // TODO: Coordinate with CaseTree handling: visitCase should leave *two* elements on the stack, so that this method can rewind and pick up separate effects for each expression and case body, and stick them together. Actually, given that, it wouldn't be too much more work to handle fall-through if I can just determine whether or not each case falls through. Maybe the CFG component has some existing stuff I can use for that.
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Void visitThrow(ThrowTree node, Void p) {
        // TODO extension
        throw new UnsupportedOperationException("not yet implemented");
        /*
         * TODO: Need to add map from exception type (Class) to pre-throw effect.
         * TODO: Throw captures current path effect... ah, but should capture from most recent /relevant/ mark, which will usually be start of method but might be start of loop (to fix loops).... I guess the right solution is for the throws to be tracked as part of the ContextEffect.... And this affects early return as well (e.g., return from within loop).  So really we need a way to mark control points (i.e. prompt boundaries).
         * We also need to think carefully about how residual checks integrate with throws --- a residual check on a behavior leading up to a throw can be incorrect.  But: if we have a meta-annotation for throw behaviors, the residual check can give an error only if the residual is undefined w.r.t *all* possible behaviors (i.e., all throws and early returns).
        */
    }

    @Override
    public Void visitTry(TryTree node, Void p) {
        // TODO extension
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        // TODO extension
        effStack.peek().mark();
        scan(node.getExpression(), p);
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        // TODO extension
        effStack.peek().mark();
        scan(node.getExpression(), p);
        effStack.peek().squashMark(node);
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitUnionType(UnionTypeTree node, Void p) {
        if (extension.doesUnionTypeCheck()) {
            effStack.peek().pushEffect(extension.checkUnionType(node), node);
            checkResidual(node);
        }
        return p;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        // Set mark for full expression
        effStack.peek().mark();

        scan(node.getCondition(), p);
        Class<? extends Annotation> condEff = effStack.peek().latestEffect();
        scan(node.getStatement(), p);
        Class<? extends Annotation> bodyEff = effStack.peek().latestEffect();

        // Here we DO NOT simply squash, because we must invoke iteration
        LinkedList<Class<? extends Annotation>> pieces = effStack.peek().rewindToMark();
        assert (pieces.get(0) == condEff);
        assert (pieces.get(1) == bodyEff);

        Class<? extends Annotation> repeff = genericEffect.iter(genericEffect.seq(bodyEff, condEff));
        if (repeff == null) {
            checker.reportError(node, "undefined.repetition.twopart", bodyEff, condEff);
            // Pretend we ran the condition, loop, and condition again
            effStack.peek().pushEffect(genericEffect.seq(condEff, genericEffect.seq(bodyEff, condEff)), node);
        } else {
            // Valid iteration
            Class<? extends Annotation> eff = genericEffect.seq(condEff, repeff);
            effStack.peek().pushEffect(eff, node);
        }
        checkResidual(node);
        return p;
    }

    @Override
    public Void visitWildcard(WildcardTree node, Void p) {
        if (extension.doesWildcardCheck()) {
            effStack.peek().pushEffect(extension.checkWildcard(node), node);
            checkResidual(node);
        }
        return p;
    }
}
