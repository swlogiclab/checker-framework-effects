package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
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
import com.sun.source.tree.IdentifierTree;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.genericeffects.qual.Impossible;
import org.checkerframework.checker.genericeffects.qual.ThrownEffect;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * GenericEffectVisitor is a base class for effect systems, including sequential effect systems.
 *
 * <p>The general idea is for checking the effect of a method to proceed by initializing an
 * accumulator to the specific system's unit effect ({@link EffectQuantale#unit()}), then
 * recursively traverse the AST to accumulate the overall effect of various subtrees. Each visit
 * method should leave the accumulator holding the effect of (only) the AST node visited.
 *
 * <p>Because methods can actually nest (if a method includes an anonymous inner class with a method
 * definition), we actually keep a <i>stack</i> of accumulators. The top element of the stack is the
 * accumulator for the current context.
 *
 * <p>The stack depth changes whenever a new AST node is visited. Upon entry to any visit method,
 * the top-most element of the stack should contain the effect of the method so far (i.e., the
 * preceeding context effect), which will aid (with further extension) in precise error reporting
 * for sequential effect systems. Upon exit, the method should leave the stack the same depth as
 * upon entry..... HMM, this is getting messy, there's a tension here between updating the stack
 * consistently so subexpressions can give good error reporting, and keeping things separate so
 * non-sequential composition (e.g., conditionals/switch) can compute the right upper bound to leave
 * for the next thiing.... or maybe snapshotting and exploring all paths is enough due to distrib
 * laws? Except for loops... and then exceptions, no we need modular tracking to assign effects to
 * individual subexpressions. So maybe we need a stack of stacks, error reporting from accumulating
 * across top-most stack :-p TODO: Add methods to the GenericEffectChecker to configure default
 * upper bounds on static and instance field initializers (static runs anywhere, field runs with
 * <i>every</i> ctor).
 */
public class GenericEffectVisitor<X> extends BaseTypeVisitor<GenericEffectTypeFactory<X>> {

  /** Debug flag, set via the "debugSpew" lint option */
  protected final boolean debugSpew;
  /** Reference to the effect quantale being checked. */
  private EffectQuantale<X> genericEffect;
  /** Reference to a plugin for determining the effects of basic Java language features. */
  private GenericEffectExtension<X> extension;

  /**
   * A stack of effect contexts, one for each level of nested methods (to support anonymous inner
   * classes).
   */
  protected final Deque<ContextEffect<X>> effStack;
  /**
   * A stack of references to the methods being processed, including null for field initialization
   * and static initializer blocks.
   */
  protected final Deque<MethodTree> currentMethods;

  /** Flag to disable effect checking */
  boolean ignoringEffects;
  /** Flag to disable warnings */
  boolean ignoringWarnings;
  /** Flag to disable errors */
  boolean ignoringErrors;

  /** Flag indicating whether the current path has already reported a type error. */
  boolean errorOnCurrentPath;

  /** Specialized reference to type factory that knows the representation type of effects. */
  GenericEffectTypeFactory<X> xtypeFactory;

  GenericEffectChecker<X> xchecker;

  private Function<Class<? extends Annotation>, X> fromAnnotation;

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
  public GenericEffectVisitor(
      GenericEffectChecker<X> checker,
      GenericEffectExtension<X> ext,
      Function<Class<? extends Annotation>, X> fromAnno) {
    super(checker);
    xchecker = checker;
    debugSpew = checker.getLintOption("debugSpew", false);

    fromAnnotation = fromAnno;
    xtypeFactory.setConversion(fromAnno);

    /* ErrorProne JdkObsolete warnings are suppressed here because we must use a deque/stack implementation that permits null.
     * Without supressing this warning, ErrorProne complains we should be using ArrayDeque, which rejects null elements. */
    effStack = new LinkedList<ContextEffect<X>>();
    currentMethods = new LinkedList<MethodTree>();

    extension = ext;

    ignoringEffects = checker.getOption("ignoreEffects") != null;
    ignoringWarnings = checker.getOption("ignoreWarnings") != null;
    ignoringErrors = checker.getOption("ignoreErrors") != null;
    errorOnCurrentPath = false;

    genericEffect = checker.getEffectLattice();

    if (debugSpew) {
      System.err.println(
          "Loading generic effect visitor with effect quantale: "
              + genericEffect.getClass().toString());
      System.err.println(
          "Loading generic effect visitor with type factory: "
              + xtypeFactory.getClass().toString());
    }
  }

  /**
   * Method to instantiate the factory class for the checker.
   *
   * @return The type factory of the checker.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected GenericEffectTypeFactory<X> createTypeFactory() {
    // This unchecked case is present because this method is invoked from the supertype's
    // constructor, so only checker (as BaseTypeChecker) is set. But since it's set from a ctor
    // argument that must be a GenericEffectChecker with appropriate effect representation, this is
    // always correct.
    // This is called from the super constructor, so xchecker is not yet set
    xtypeFactory =
        new GenericEffectTypeFactory<X>(
            (GenericEffectChecker<X>) checker, checker.getLintOption("debugSpew", false));
    return xtypeFactory;
  }

  /**
   * TODO: Please document the use off this with respect to the generic effect checker better. Note:
   * The GuiEffectChecker uses a similar setup and provides more documentation.
   *
   * @param node Class declaration to process
   */
  @Override
  public void processClassTree(ClassTree node) {
    // Fix up context for static initializers of new class
    currentMethods.addFirst(null);
    effStack.addFirst(new ContextEffect<X>(genericEffect));
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
    // Save and restore errorOnCurrentPath, so methods of anonymous inner classes
    // don't inherit contextual errors from their allocating contexts
    boolean contextualErrorOnCurrentPath = errorOnCurrentPath;
    errorOnCurrentPath = false;

    ExecutableElement methElt = TreeUtils.elementFromDeclaration(node);
    if (debugSpew) {
      System.err.println("\nVisiting method " + methElt);
    }

    assert (methElt != null);

    // Override check
    xtypeFactory.checkEffectOverride(
        (TypeElement) methElt.getEnclosingElement(), methElt, true, node);

    Map<Class<? extends Exception>, X> excBehaviors = new HashMap<>();
    // Check that any @ThrownEffect uses are valid
    for (AnnotationMirror thrown : xtypeFactory.getDeclAnnotations(methElt)) {
      if (xtypeFactory.areSameByClass(thrown, ThrownEffect.class)) {
        ThrownEffect thrownEff = (ThrownEffect) thrown;
        // TODO: require the effect be a checked exception (i.e., not subtype of RuntimeException)
        X prev =
            excBehaviors.put(thrownEff.exception(), fromAnnotation.apply(thrownEff.behavior()));
        if (prev != null) {
          checker.reportError(
              node,
              "duplicate.annotation.thrown",
              thrownEff.exception(),
              thrownEff.behavior(),
              prev);
        }
      }
    }

    // Initialize method stack
    currentMethods.addFirst(node);
    effStack.addFirst(new ContextEffect<X>(genericEffect));

    if (debugSpew) {
      System.err.println(
          "Pushing " + effStack.peekFirst() + " onto the stack when checking " + methElt);
    }

    Void ret = super.visitMethod(node, p);

    // Completion Check
    // We skip this if every path to the end of the method already reported a type (effect) error
    // TODO: This isn't *quite* what we want for *commutative* effect systems, for which we'd like
    // to report *all* errors...
    // TODO: Maybe an extra flag on the lattice, so we handle non-comm differently from comm
    // systems?
    // TODO: Work out laws for residuals w/ comm: e.g., x\(y\z) def <-> y\(x\z) def?
    if (!errorOnCurrentPath) {
      X targetEffect = effStack.peek().currentPathEffect();
      X callerEffect = xtypeFactory.getDeclaredEffect(methElt);
      if (!effStack.peek().currentlyImpossible() && isInvalid(targetEffect, callerEffect))
        checkError(node, targetEffect, callerEffect, "subeffect.invalid.methodbody");
      else if (!effStack.peek().currentlyImpossible() && extension.reportWarning(node) != null)
        checkWarning(node, targetEffect, callerEffect, extension.reportWarning(node));
    }

    currentMethods.removeFirst();
    effStack.removeFirst();
    if (debugSpew) {
      System.err.println("Finished visiting method " + methElt + "\n");
    }

    errorOnCurrentPath = contextualErrorOnCurrentPath;

    return ret;
  }

  /**
   * Method that can be used in a visitor method to see if a node is enclosed by a method.
   *
   * @return A boolean representing whether the node is enclosed by a method (true) or not (false).
   */
  @SuppressWarnings("UnusedMethod")
  private boolean hasEnclosingMethod() {
    MethodTree callerTree = TreePathUtil.enclosingMethod(getCurrentPath());
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
  private boolean isInvalid(X targetEffect, X callerEffect) {
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
      Tree node, X targetEffect, X callerEffect, @CompilerMessageKey String failureMsg) {
    if (!ignoringErrors) checker.reportError(node, failureMsg, targetEffect, callerEffect);
    else if (ignoringErrors && !extension.isIgnored(checker.getOption("ignoreErrors"), failureMsg))
      checker.reportError(node, failureMsg, targetEffect, callerEffect);
  }

  private void checkResidual(Tree node) {
    // TODO: Impose actual checks on static & instance field initializer expression effects
    if (currentMethods.peek() == null) {
      return;
    }
    // Skip the check if we've already reported an error on this path.
    if (!ignoringErrors && !errorOnCurrentPath) {
      X pathEffect = effStack.peek().currentPathEffect();
      if (pathEffect == Impossible.class) {
        return; // In an enclosing context of a path that always throws/returns
      }
      X methodEffect =
          xtypeFactory.getDeclaredEffect(TreeUtils.elementFromDeclaration(currentMethods.peek()));
      if (debugSpew) {
        System.err.println("Checking residual " + pathEffect + " \\ " + methodEffect);
        System.err.println("In location " + visitorState.getPath());
      }
      if (genericEffect.residual(pathEffect, methodEffect) == null) {
        if (genericEffect.isCommutative()) {
          // For commutative systems, we clean up the error message by peeking at the last addition
          checker.reportError(
              node, "operation.invalid", effStack.peek().latestEffect(), methodEffect);
          // For commutative systems, we *don't* set the current path error flag, but do reset the
          // accumulator so future residual checks *also* yield errors (we know they should since
          // sequencing is commutative)
          effStack.peek().rewriteLastEffectToCommutativeUnit();
        } else {
          checker.reportError(node, "undefined.residual", pathEffect, methodEffect);
          errorOnCurrentPath = true;
        }
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
      Tree node, X targetEffect, X callerEffect, @CompilerMessageKey String warningMsg) {
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
  private X getMethodCallerEffect() {
    MethodTree callerTree = TreePathUtil.enclosingMethod(getCurrentPath());
    ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
    return xtypeFactory.getDeclaredEffect(callerElt);
  }

  /**
   * Method that is used in a visitor method to get the default effect a class that a node is
   * within.
   *
   * <p>TODO: Must split between static vs. instance field initializers
   *
   * @return The default effect of a class that a node is within.
   */
  @SuppressWarnings("UnusedMethod")
  private X getDefaultClassEffect() {
    ClassTree clsTree = TreePathUtil.enclosingClass(getCurrentPath());
    Element clsElt = TreeUtils.elementFromDeclaration(clsTree);
    return xtypeFactory.getDefaultEffect(clsElt);
  }

  /**
   * Method that visits all the method invocation tree nodes and raises failures/warnings for unsafe
   * method invocations.
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
    for (Tree args : node.getArguments()) {
      scan(args, p);
    }
    ExecutableElement methodElt = TreeUtils.elementFromUse(node);
    X targetEffect = xtypeFactory.getDeclaredEffect(methodElt);
    if (debugSpew) {
      System.err.println("Pushing latent effect " + targetEffect + " for " + node);
    }
    effStack.peek().pushEffect(targetEffect, node);
    if (debugSpew) {
      System.err.println(
          "Path effect after " + node + " BEFORE squash is " + effStack.peek().currentPathEffect());
    }
    effStack.peek().squashMark(node);
    if (debugSpew) {
      System.err.println(
          "Path effect after " + node + " AFTER squash is " + effStack.peek().currentPathEffect());
      System.err.println("In location " + TreePathUtil.toString(visitorState.getPath()));
      System.err.println(
          "Static scope? " + TreePathUtil.isTreeInStaticScope(visitorState.getPath()));
    }
    checkResidual(node);
    return p;
  }

  /**
   * Method to compute the effect of a variable access.
   * 
   * This might seem unnecessary, but many parts of this visitor assume visiting any tree pushes some kind of effect onto the stack.
   */
  @Override
  public Void visitIdentifier(IdentifierTree node, Void p) {
    effStack.peek().pushEffect(genericEffect.unit(), node);
    // No need to check anything, we just pushed unit
    return p;
  }

  /**
   * Method to check if the constructor call is made from a valid context.
   *
   * <p>TODO: Fix for static vs. instance initializers
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
    // for (Tree args : node.getArguments()) {
    //  scan(args, p);
    // }
    // Visit arguments, and if anonymous inner class, the inner class body
    super.visitNewClass(node, p);
    ExecutableElement methodElt = TreeUtils.elementFromUse(node);
    X targetEffect = xtypeFactory.getDeclaredEffect(methodElt);
    effStack.peek().pushEffect(targetEffect, node);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  /**
   * The methods below this comment follow the same format. Each method is a different visit method
   * for a different kind of tree node. Using the extensions class the developer can activate
   * specific visitor methods depending on what they want to check.
   *
   * <p>The methods work by first checking if the node being checked is enclosed by a method. If it
   * is then the method obtains the effect of the node and checks it against the method's effect. If
   * the node is not enclosed by a method, then it checks at the variable level against the class
   * annotation.
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
    X condEff = effStack.peek().squashMark(node);
    X joinWithUnit = genericEffect.LUB(genericEffect.unit(), condEff);
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
    ;
    return p;
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
    X bodyEff = effStack.peek().latestEffect();
    scan(node.getCondition(), p);
    X condEff = effStack.peek().latestEffect();

    // Here we DO NOT simply squash, because we must invoke iteration
    LinkedList<X> pieces = effStack.peek().rewindToMark();
    assert (pieces.get(0) == bodyEff);
    assert (pieces.get(1) == condEff);

    X repeff = genericEffect.iter(genericEffect.seq(bodyEff, condEff));
    if (repeff == null) {
      checker.reportError(node, "undefined.repetition.twopart", bodyEff, condEff);
      // Pretend we ran the loop and condition once each
      boolean success = effStack.peek().pushEffect(genericEffect.seq(bodyEff, condEff), node);
      assert success; // TODO fix
    } else {
      // Valid iteration
      X eff = genericEffect.seq(condEff, repeff);
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
    X initEff = effStack.peek().latestEffect();
    scan(node.getCondition(), p);
    X condEff = effStack.peek().latestEffect();
    scan(node.getStatement(), p);
    X bodyEff = effStack.peek().latestEffect();
    // mark for updates, since there may be multiple
    effStack.peek().mark();
    scan(node.getUpdate(), p);
    X updateEff = effStack.peek().squashMark(null);

    // If we're reached here, it's possible to run the initializers, cond, body, update in that
    // order
    // We care about iterating body-update-cond, though

    // Here we DO NOT simply squash, because we must invoke iteration
    LinkedList<X> pieces = effStack.peek().rewindToMark();
    assert (pieces.get(0) == initEff);
    assert (pieces.get(1) == condEff);
    assert (pieces.get(2) == bodyEff);
    assert (pieces.get(3) == updateEff);

    X repeff =
        genericEffect.iter(genericEffect.seq(genericEffect.seq(bodyEff, updateEff), condEff));
    if (repeff == null) {
      checker.reportError(node, "undefined.repetition.threepart", bodyEff, updateEff, condEff);
      // Pretend we ran the loop exactly once: init, condition, loop, update, and condition again
      effStack
          .peek()
          .pushEffect(genericEffect.seq(condEff, genericEffect.seq(bodyEff, condEff)), node);
    } else {
      // Valid iteration
      X eff = genericEffect.seq(genericEffect.seq(initEff, condEff), repeff);
      effStack.peek().pushEffect(eff, node);
    }
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitIf(IfTree node, Void p) {
    return checkConditional(
        node, node.getCondition(), node.getThenStatement(), node.getElseStatement(), p);
  }

  protected Void checkConditional(
      Tree node, ExpressionTree condTree, Tree thenTree, Tree elseTree, Void p) {

    // One mark for the whole node, nested mark for each branch.
    effStack.peek().mark();
    scan(condTree, p);
    boolean condError = errorOnCurrentPath;
    effStack.peek().mark();
    scan(thenTree, p);
    boolean thenError = errorOnCurrentPath;
    LinkedList<X> thenEffs = effStack.peek().rewindToMark();
    assert (thenEffs.size() == 1);
    X thenEff = thenEffs.get(0);
    X elseEff = genericEffect.unit();
    // If there's no else, there's no else error
    boolean elseError = false;
    // If there was an error on the then-path, there may still be no issue on the else path
    errorOnCurrentPath = condError;
    if (elseTree != null) {
      effStack.peek().mark();
      scan(elseTree, p);
      elseError = errorOnCurrentPath;
      LinkedList<X> elseEffs = effStack.peek().rewindToMark();
      assert (elseEffs.size() == 1);
      elseEff = elseEffs.get(0);
    }
    // stack still has the condition effect on it, but no branch effects
    LinkedList<X> condEffs = effStack.peek().rewindToMark();
    assert (condEffs.size() == 1);
    X condEff = condEffs.get(0);

    errorOnCurrentPath = condError || (thenError && elseError);

    if ((thenEff == Impossible.class && elseEff == Impossible.class) || errorOnCurrentPath) {
      // Both branches return and/or throw, so regular paths through this term
      // do not return normally
      effStack.peek().markImpossible(node);
    } else if (thenEff == Impossible.class || thenError) {
      effStack.peek().pushEffect(genericEffect.seq(condEff, elseEff), node);
    } else if (elseEff == Impossible.class || elseError) {
      effStack.peek().pushEffect(genericEffect.seq(condEff, thenEff), node);
    } else {
      // Both branches possible and error-free: the common case
      X lub = genericEffect.LUB(thenEff, elseEff);
      if (lub == null) {
        if (elseTree == null) {
          checker.reportError(node, "undefined.join.unaryif", thenEff, elseEff);
        } else {
          checker.reportError(node, "undefined.join", thenEff, elseEff);
        }
        errorOnCurrentPath = true;
      }

      // This seq will always succeed (with valid EQs) since the seqs worked per-branch, and we have
      // distributivity
      effStack.peek().pushEffect(genericEffect.seq(condEff, lub), node);

      // TODO: Figure out when we do/don't want multiple errors issued. Clearly want multiple for
      // cases like traditional LUB systems, but sometimes may want to stop early for truly
      // sequential EQs
      checkResidual(node);
    }
    return p;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
    // TODO extension checks
    return checkConditional(
        node, node.getCondition(), node.getTrueExpression(), node.getFalseExpression(), p);
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
    // Note: We don't iterate even if there is a single initializer for all array cells, because the
    // expression is evaluated only once, and the value is duplicated
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
    // TODO: Need to handle early returns!!! This currently only handles return from tail position.
    // Exceptions I think are reasonable to punt on for now, but early returns are not
    // TODO: Perhaps I can just keep a stack of "early return effects", which I can handle by just
    // adding the effect so far to that stack? Oh, actually, don't even need that. There's no
    // residual check here, just a LE check!
    effStack.peek().mark();
    scan(node.getExpression(), p);
    effStack.peek().squashMark(node);
    // TODO: is looking at the top of the stack faster than getMethodCallerEffect ?
    if (!genericEffect.LE(effStack.peek().currentPathEffect(), getMethodCallerEffect())) {
      checker.reportError(
          node, "invalid.return", effStack.peek().currentPathEffect(), getMethodCallerEffect());
    }
    // TODO: Real question is what state I leave the stack in when returning. This will be hit
    // always at the end of a sequence of statements, but sometimes those will be nested inside a
    // conditional or loop.... and want the callers to know not to consider this path --- poison
    // value that can be inspected e.g. in conditional cases, which already save&restore?
    // TODO: Maybe the "set of behaviors" collection is the right way to handle exceptions, as long
    // as I track which exception leads to what... but then I need to handle methods that return
    // effects.... so I need a meta-annotation @ThrowsEffect(Class<?>, X)!
    effStack.peek().markImpossible(node);
    return p;
  }

  @Override
  public Void visitSwitch(SwitchTree node, Void p) {
    // TODO extension
    effStack.peek().mark();
    scan(node.getExpression(), p);
    // This is tricky: *assuming no fall-through*, we'll execute some number of case expressions,
    // followed by some (one) body, and possibly a default case.
    // TODO: Coordinate with CaseTree handling: visitCase should leave *two* elements on the stack,
    // so that this method can rewind and pick up separate effects for each expression and case
    // body, and stick them together. Actually, given that, it wouldn't be too much more work to
    // handle fall-through if I can just determine whether or not each case falls through. Maybe the
    // CFG component has some existing stuff I can use for that.
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
    // scan(node.getExpression(), p);
    //// Note: we explicitly do *not* check the residual here!
    //// In principle we could check residuals (against @ThrownEffect annotations) when the thrown
    // exception is a type we know is in the
    //// @ThrownEffects set *and* we're not inside a try-catch that handles the same effect
    // *and*....
    //// TODO: Fix this to check *exception* residuals when possible and valid. For now we fall back
    // to global checks for throws
    //// TODO: Actually, not residuals, but flat out completion for exceptional returns

    //// TODO: This won't be quite right yet: we really want to track throws up to a particular
    // mark, OR we want a way to restore a full stack with marks. Determine based on try
    // effStack.peek().trackExplicitThrow((Class<? extends
    // Exception>)TypesUtils.getClassFromType(TreeUtils.typeOf(node.getExpression())), node);
    // effStack.peek().markImpossible(node);
    // return p;
  }

  @Override
  public Void visitTry(TryTree node, Void p) {
    // TODO extension
    throw new UnsupportedOperationException("not yet implemented");

    // BlockTree body = node.getBlock();
    // BlockTree finblock = node.getFinallyBlock();
    // var catches = node.getCatches();

    // TODO: Okay, can't just append finally block effect to all entries in the exception map,
    // because there might be some in there from another branch of execution (e.g., the then branch
    // of a conditional, where this try is in the else block). The solution is to properly implement
    // C(X).

  }

  @Override
  public Void visitTypeCast(TypeCastTree node, Void p) {
    effStack.peek().mark();
    scan(node.getExpression(), p);
    effStack.peek().pushEffect(extension.checkTypeCast(node), node);
    effStack.peek().squashMark(node);
    checkResidual(node);
    String warning = extension.reportWarning(node);
    if (warning != null) {
      checker.reportWarning(node, warning);
    }
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
  public Void visitBlock(BlockTree node, Void p) {
    effStack.peek().mark();
    super.visitBlock(node,p);
    effStack.peek().squashMark(node);
    return p;
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree node, Void p) {
    // Set mark for full expression
    effStack.peek().mark();
    scan(node.getCondition(), p);
    X condEff = effStack.peek().latestEffect();
    scan(node.getStatement(), p);
    X bodyEff = effStack.peek().latestEffect();

    // Here we DO NOT simply squash, because we must invoke iteration
    LinkedList<X> pieces = effStack.peek().rewindToMark();
    assert (pieces.get(0) == condEff);
    assert (pieces.get(1) == bodyEff);

    X repeff = genericEffect.iter(genericEffect.seq(bodyEff, condEff));
    if (repeff == null) {
      checker.reportError(node, "undefined.repetition.twopart", bodyEff, condEff);
      // Pretend we ran the condition, loop, and condition again
      effStack
          .peek()
          .pushEffect(genericEffect.seq(condEff, genericEffect.seq(bodyEff, condEff)), node);
    } else {
      // Valid iteration
      X eff = genericEffect.seq(condEff, repeff);
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
