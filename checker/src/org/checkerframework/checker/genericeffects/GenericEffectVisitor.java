package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Stack;
import javax.lang.model.element.*;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

public class GenericEffectVisitor extends BaseTypeVisitor<GenericEffectTypeFactory> {

    protected final boolean debugSpew;
    private GenericEffectLattice genericEffect;
    private GenericEffectExtension extension;
    // effStack and currentMethods should always be the same size.
    protected final Stack<Class<? extends Annotation>> effStack;
    protected final Stack<MethodTree> currentMethods;

    //fields for variables
    protected final Stack<Class<? extends Annotation>> varEffStack;
    protected final Stack<VariableTree> currentVars;

    /**
     * Constructor that takes passes the checker to the superclass and takes in a GenericEffectExtension object.
     * The effect stack for methods and variables are set up in the constructor.
     *
     * @param checker The checker that allows the Casting Effects Checker to function.
     * @param ext An GenericEffectExtension object that provides the developer with more functions dealing with specific tree nodes.
     */
    public GenericEffectVisitor(BaseTypeChecker checker, GenericEffectExtension ext) {
        super(checker);
        assert (checker instanceof GenericEffectChecker);
        debugSpew = checker.getLintOption("debugSpew", false);

        effStack = new Stack<Class<? extends Annotation>>();
        currentMethods = new Stack<MethodTree>();

        //added this assignment
        varEffStack = new Stack<Class<? extends Annotation>>();;
        currentVars = new Stack<VariableTree>();
        extension = ext;

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

    //TODO: Determine what the use of this method is.
    @Override
    protected void checkMethodInvocability(
            AnnotatedExecutableType method, MethodInvocationTree node) {
    }

    //TODO: Determine what the use of this method is.
    @Override
    protected boolean checkOverride(
            MethodTree overriderTree,
            AnnotatedTypeMirror.AnnotatedDeclaredType enclosingType,
            AnnotatedTypeMirror.AnnotatedExecutableType overridden,
            AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType,
            Void p) {
        // Method override validity is checked manually by the type factory during visitation
        return true;
    }

    //TODO: Determine what the use of this method is.
    @Override
    public void processClassTree(ClassTree node) {
        // Fix up context for static initializers of new class
        currentMethods.push(null);
        effStack.push(genericEffect.getBottomMostEffectInLattice());
        super.processClassTree(node);
        currentMethods.pop();
        effStack.pop();
    }

    /**
     * Method that visits method tree nodes and adds their effects to the stacks set up in the constructor.
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

        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = atypeFactory.getDeclAnnotation(methElt, OkEffect);

            (atypeFactory)
                    .checkEffectOverride(
                            (TypeElement) (methElt.getEnclosingElement()), methElt, true, node);

            if (annotatedEffect == null) {
                atypeFactory
                        .fromElement(methElt)
                        .addAnnotation(atypeFactory.getDeclaredEffect(methElt));
            }
        }

        currentMethods.push(node);

        effStack.push(atypeFactory.getDeclaredEffect(methElt));

        if (debugSpew) {
            System.err.println(
                    "Pushing " + effStack.peek() + " onto the stack when checking " + methElt);
        }

        Void ret = super.visitMethod(node, p);
        currentMethods.pop();
        effStack.pop();
        return ret;
    }

    /**
     * Method that visits variable tree nodes and adds their effects to the stacks set up in the constructor.
     *
     * @param node The variable tree node that was encountered during checking.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitVariable(VariableTree node, Void p)
    {
        Element methElt = TreeUtils.elementFromDeclaration(node);


        assert (methElt != null);

        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = atypeFactory.getDeclAnnotation(methElt, OkEffect);

            if (annotatedEffect == null) {
                atypeFactory
                        .fromElement(methElt)
                        .addAnnotation(atypeFactory.getDeclaredEffect(methElt));
            }
        }

        currentVars.push(node);

        varEffStack.push(atypeFactory.getDeclaredEffect(methElt));


        Void ret = super.visitVariable(node, p);
        currentVars.pop();
        varEffStack.pop();
        return ret;

    }

    /**
     * Method that can be used in a visitor method to see if a node is enclosed by a method.
     *
     * @return A boolean representing whether the node is enclosed by a method (true) or not (false).
     */
    private boolean hasEnclosingMethod() {
        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        return callerTree != null;
    }

    /**
     * Method that is used by all visitors to enforce the checker's lattice and report errors and warnings depending on the
     * developers specifications. Compiler arguments are also checked to determine what should be ignored. This method
     * checks at the method level.
     *
     * @param node A tree node encountered during checking.
     * @param targetEffect The effect of the tree node encountered during checking.
     * @param failureMsg The failure message to be reported.
     * @param warningMsg The warning message to be reported.
     */
    private void checkEnclosingMethod(Tree node, Class<? extends Annotation> targetEffect, String failureMsg, String warningMsg) {
        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
        Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt);
        if (checker.getOption("ignoreEffects") != null)
            targetEffect = extension.checkIgnoredEffects(checker.getOption("ignoreEffects"), targetEffect);
        if(warningMsg != null) {
            if(checker.getOption("ignoreWarnings") == null)
                checker.report(Result.warning(warningMsg, targetEffect, callerEffect), node);
            else if (checker.getOption("ignoreWarnings") != null && !extension.isIgnored(checker.getOption("ignoreWarnings"), warningMsg))
                checker.report(Result.warning(warningMsg, targetEffect, callerEffect), node);
        }
        if (!genericEffect.LE(targetEffect, callerEffect)) {
            if(checker.getOption("ignoreErrors") == null)
                checker.report(Result.failure(failureMsg, targetEffect, callerEffect), node);
            else if (checker.getOption("ignoreErrors") != null && !extension.isIgnored(checker.getOption("ignoreErrors"), failureMsg))
                checker.report(Result.failure(failureMsg, targetEffect, callerEffect), node);
        }
    }

    /**
     * Method that can be used in a visitor method to see if a node is enclosed by a variable.
     *
     * @return A boolean representing whether the node is enclosed by a variable (true) or not (false).
     */
    private boolean hasEnclosingVariable() {
        VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
        return varTree != null;
    }

    /**
     * Method that is used by all visitors to enforce the checker's lattice and report errors and warnings depending on the
     * developers specifications. Compiler arguments are also checked to determine what should be ignored. This method
     * checks at the variable level.
     *
     * @param node A tree node encountered during checking.
     * @param targetEffect The effect of the tree node encountered during checking.
     * @param failureMsg The failure message to be reported.
     * @param warningMsg The warning message to be reported.
     */
    private void checkEnclosingVariable(Tree node, Class<? extends Annotation> targetEffect, String failureMsg, String warningMsg) {
        VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
        VariableElement varElt = TreeUtils.elementFromDeclaration(varTree);
        Class<? extends Annotation> varCallerEffect = atypeFactory.getDeclaredEffect(varElt);
        if(warningMsg != null) {
            if(checker.getOption("ignoreWarnings") == null)
                checker.report(Result.warning(warningMsg, targetEffect, varCallerEffect), node);
            else if (checker.getOption("ignoreWarnings") != null && !extension.isIgnored(checker.getOption("ignoreWarnings"), warningMsg))
                checker.report(Result.warning(warningMsg, targetEffect, varCallerEffect), node);
        }
        if (!genericEffect.LE(targetEffect, varCallerEffect)) {
            if(checker.getOption("ignoreErrors") == null)
                checker.report(Result.failure(failureMsg, targetEffect, varCallerEffect), node);
            else if (checker.getOption("ignoreErrors") != null && !extension.isIgnored(checker.getOption("ignoreErrors"), failureMsg))
                checker.report(Result.failure(failureMsg, targetEffect, varCallerEffect), node);
        }
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
        //TODO: This is not supported yet.
        return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree node, Void p) {
        //TODO: Determine if this requires the same effect checks as for methods.
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        //TODO: Determine if this requires the same effect checks as for methods.
        return null;
    }

    /**
     * Method that visits all the method invocation tree nodes and raises failures/warnings for unsafe method invocations.
     *
     * @param node Method invocation tree node that is found during checking.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if(hasEnclosingMethod()) {
            ExecutableElement methodElt = TreeUtils.elementFromUse(node);
            Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt);
            checkEnclosingMethod(node,
                    targetEffect,
                    "call.invalid.super.effect",
                    null);
            return super.visitMethodInvocation(node, p);
        }
        if(hasEnclosingVariable()) {
            ExecutableElement varElt = TreeUtils.elementFromUse(node);
            Class<? extends Annotation> varTargetEffect = atypeFactory.getDeclaredEffect(varElt);
            checkEnclosingVariable(node,
                    varTargetEffect,
                    "call.invalid.super.effect",
                    null);
            return super.visitMethodInvocation(node, p);
        }
        return super.visitMethodInvocation(node, p);
    }

    /**
     * Method to check if the constructor call is made from a valid context.
     *
     * @param node New class tree node that is found during checking.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        if(hasEnclosingMethod()) {
            ExecutableElement methodElt = TreeUtils.elementFromUse(node);
            Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt);
            checkEnclosingMethod(node,
                    targetEffect,
                    "constructor.call.invalid",
                    null);
            return super.visitNewClass(node, p);
        }
        if(hasEnclosingVariable()) {
            ExecutableElement varElt = TreeUtils.elementFromUse(node);
            Class<? extends Annotation> varTargetEffect = atypeFactory.getDeclaredEffect(varElt);
            checkEnclosingVariable(node,
                    varTargetEffect,
                    "constructor.call.invalid",
                    null);
            return super.visitNewClass(node, p);
        }
        return super.visitNewClass(node, p);
    }

    /**
     * The methods below this comment follow the same format. Each method is a different visit method
     * for a different kind of tree node. Using the extensions class the developer can activate specific
     * visitor methods depending on what they want to check.
     *
     * The methods work by first checking if the node being checked is enclosed by a method. If it is then
     * the method obtains the effect of the node and checks it against the method's effect. If the node is not
     * enclosed by a method, then it checks at the variable level against the class annotation.
     *
     * @param node Specific tree node that is to be checked.
     * @param p Void
     * @return Void
     */
    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        if(extension.doesArrayAccessCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkArrayAccess(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitArrayAccess(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkArrayAccess(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitArrayAccess(node, p);
            }
            return super.visitArrayAccess(node, p);
        }
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree node, Void p) {
        if(extension.doesArrayTypeCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkArrayType(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitArrayType(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkArrayType(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitArrayType(node, p);
            }
            return super.visitArrayType(node, p);
        }
        return null;
    }

    @Override
    public Void visitAssert(AssertTree node, Void p) {
        if(extension.doesAssertCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkAssert(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitAssert(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkAssert(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitAssert(node, p);
            }
            return super.visitAssert(node, p);
        }
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        if(extension.doesAssignmentCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkAssignment(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitAssignment(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkAssignment(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitAssignment(node, p);
            }
            return super.visitAssignment(node, p);
        }
        return null;
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        if(extension.doesBinaryCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkBinary(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitBinary(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkBinary(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitBinary(node, p);
            }
            return super.visitBinary(node, p);
        }
        return null;
    }

    @Override
    public Void visitBreak(BreakTree node, Void p) {
        if(extension.doesBreakCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkBreak(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitBreak(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkBreak(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitBreak(node, p);
            }
            return super.visitBreak(node, p);
        }
        return null;
    }

    @Override
    public Void visitCase(CaseTree node, Void p) {
        if(extension.doesCaseCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkCase(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitCase(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkCase(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitCase(node, p);
            }
            return super.visitCase(node, p);
        }
        return null;
    }

    @Override
    public Void visitCatch(CatchTree node, Void p) {
        if(extension.doesCatchCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkCatch(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitCatch(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkCatch(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitCatch(node, p);
            }
            return super.visitCatch(node, p);
        }
        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        if(extension.doesCompoundAssignmentCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkCompoundAssignment(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitCompoundAssignment(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkCompoundAssignment(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitCompoundAssignment(node, p);
            }
            return super.visitCompoundAssignment(node, p);
        }
        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        if(extension.doesConditionalExpressionCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkConditionalExpression(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitConditionalExpression(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkConditionalExpression(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitConditionalExpression(node, p);
            }
            return super.visitConditionalExpression(node, p);
        }
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree node, Void p) {
        if(extension.doesContinueCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkContinue(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitContinue(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkContinue(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitContinue(node, p);
            }
            return super.visitContinue(node, p);
        }
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        if(extension.doesDoWhileLoopCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkDoWhileLoop(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitDoWhileLoop(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkDoWhileLoop(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitDoWhileLoop(node, p);
            }
            return super.visitDoWhileLoop(node, p);
        }
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        if(extension.doesEnhancedForLoopCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkEnhancedForLoop(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitEnhancedForLoop(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkEnhancedForLoop(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitEnhancedForLoop(node, p);
            }
            return super.visitEnhancedForLoop(node, p);
        }
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void p) {
        if(extension.doesForLoopCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkForLoop(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitForLoop(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkForLoop(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitForLoop(node, p);
            }
            return super.visitForLoop(node, p);
        }
        return null;
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        if(extension.doesIfCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkIf(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitIf(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkIf(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitIf(node, p);
            }
            return super.visitIf(node, p);
        }
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {
        if(extension.doesInstanceOfCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkInstanceOf(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitInstanceOf(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkInstanceOf(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitInstanceOf(node, p);
            }
            return super.visitInstanceOf(node, p);
        }
        return null;
    }

    @Override
    public Void visitIntersectionType(IntersectionTypeTree node, Void p) {
        if(extension.doesIntersectionTypeCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkIntersectionType(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitIntersectionType(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkIntersectionType(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitIntersectionType(node, p);
            }
            return super.visitIntersectionType(node, p);
        }
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree node, Void p) {
        if(extension.doesLabeledStatementCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkLabeledStatement(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitLabeledStatement(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkLabeledStatement(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitLabeledStatement(node, p);
            }
            return super.visitLabeledStatement(node, p);
        }
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void p) {
        if(extension.doesLiteralCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkLiteral(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitLiteral(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkLiteral(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitLiteral(node, p);
            }
            return super.visitLiteral(node, p);
        }
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        if(extension.doesNewArrayCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkNewArray(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitNewArray(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkNewArray(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitNewArray(node, p);
            }
            return super.visitNewArray(node, p);
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree node, Void p) {
        if(extension.doesPrimitiveTypeCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkPrimitiveType(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitPrimitiveType(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkPrimitiveType(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitPrimitiveType(node, p);
            }
            return super.visitPrimitiveType(node, p);
        }
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        if(extension.doesReturnCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkReturn(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitReturn(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkReturn(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitReturn(node, p);
            }
            return super.visitReturn(node, p);
        }
        return null;
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        if(extension.doesSwitchCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkSwitch(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitSwitch(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkSwitch(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitSwitch(node, p);
            }
            return super.visitSwitch(node, p);
        }
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        if(extension.doesSynchronizedCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkSynchronized(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitSynchronized(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkSynchronized(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitSynchronized(node, p);
            }
            return super.visitSynchronized(node, p);
        }
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree node, Void p) {
        if(extension.doesThrowCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkThrow(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitThrow(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkThrow(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitThrow(node, p);
            }
            return super.visitThrow(node, p);
        }
        return null;
    }

    @Override
    public Void visitTry(TryTree node, Void p) {
        if(extension.doesTryCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkTry(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitTry(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkTry(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitTry(node, p);
            }
            return super.visitTry(node, p);
        }
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if(extension.doesTypeCastCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkTypeCast(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitTypeCast(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkTypeCast(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitTypeCast(node, p);
            }
            return super.visitTypeCast(node, p);
        }
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        if(extension.doesUnaryCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkUnary(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitUnary(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkUnary(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitUnary(node, p);
            }
            return super.visitUnary(node, p);
        }
        return null;
    }

    @Override
    public Void visitUnionType(UnionTypeTree node, Void p) {
        if(extension.doesUnionTypeCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkUnionType(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitUnionType(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkUnionType(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitUnionType(node, p);
            }
            return super.visitUnionType(node, p);
        }
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        if(extension.doesWhileLoopCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkWhileLoop(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitWhileLoop(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkWhileLoop(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitWhileLoop(node, p);
            }
            return super.visitWhileLoop(node, p);
        }
        return null;
    }

    @Override
    public Void visitWildcard(WildcardTree node, Void p) {
        if(extension.doesWildcardCheck()) {
            if(hasEnclosingMethod()) {
                Class<? extends Annotation> targetEffect = extension.checkWildcard(node);
                checkEnclosingMethod(node,
                        targetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitWildcard(node, p);
            }
            if(hasEnclosingVariable()) {
                Class<? extends Annotation> varTargetEffect = extension.checkWildcard(node);
                checkEnclosingVariable(node,
                        varTargetEffect,
                        extension.reportError(node),
                        extension.reportWarning(node));
                return super.visitWildcard(node, p);
            }
            return super.visitWildcard(node, p);
        }
        return null;
    }

}

