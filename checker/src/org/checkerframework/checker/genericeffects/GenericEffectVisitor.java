package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.Callable;
import javax.lang.model.element.*;

import com.sun.source.util.TreePath;
import org.checkerframework.checker.genericeffects.qual.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

//Type rules are defined here
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

    // Method to instantiate the factory class for the checker
    @Override
    protected GenericEffectTypeFactory createTypeFactory() {
        return new GenericEffectTypeFactory(checker, debugSpew);
    }

    @Override
    protected void checkMethodInvocability(
            AnnotatedExecutableType method, MethodInvocationTree node) {
    }

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

    // Method to check that the invoked effect is <= permitted effect (effStack.peek())
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        /*
        if (debugSpew) {
            System.err.println("For invocation " + node + " in " + currentMethods.peek().getName());
        }

        // Target method annotations
        ExecutableElement methodElt = TreeUtils.elementFromUse(node);

        if (debugSpew) {
            System.err.println("methodElt found");
        }

        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        if (callerTree == null) {
            if (debugSpew) {
                System.err.println("No enclosing method: likely static initializer");
            }
            return super.visitMethodInvocation(node, p);
        }
        if (debugSpew) {
            System.err.println("callerTree found");
        }

        ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
        if (debugSpew) {
            System.err.println("callerElt found");
        }

        Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt);
        Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt);

        if (!genericEffect.LE(targetEffect, callerEffect)) {

            checker.report(
                    Result.failure("call.invalid.super.effect", targetEffect, callerEffect), node);

            if (debugSpew) {
                System.err.println("Issuing error for node: " + node);
            }
        }
        if (debugSpew) {
            System.err.println(
                    "Successfully finished main non-recursive checkinv of invocation " + node);
        }

        return super.visitMethodInvocation(node, p);
        */


            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                //Class<? extends Annotation> targetEffect = extension.checkArrayAccess(node);
                ExecutableElement methodElt = TreeUtils.elementFromUse(node);
                //does this act correct?
                Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt, getClassTree());

                checkWithinMethod(callerTree, node, targetEffect, "call.invalid.super.effect");
                return super.visitMethodInvocation(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                //Class<? extends Annotation> varTargetEffect = extension.checkArrayAccess(node);
                ExecutableElement varElt = TreeUtils.elementFromUse(node);
                Class<? extends Annotation> varTargetEffect = atypeFactory.getDeclaredEffect(varElt, getClassTree());

                checkWithinVariable(varTree, node, varTargetEffect, "field.invalid");
                return super.visitMethodInvocation(node, p);
            }
            return super.visitMethodInvocation(node, p);
        //return null;

    }

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
                        .addAnnotation(atypeFactory.getDeclaredEffect(methElt, getClassTree()));
            }
        }

        currentMethods.push(node);

        effStack.push(atypeFactory.getDeclaredEffect(methElt, getClassTree()));

        if (debugSpew) {
            System.err.println(
                    "Pushing " + effStack.peek() + " onto the stack when checking " + methElt);
        }

        Void ret = super.visitMethod(node, p);
        currentMethods.pop();
        effStack.pop();
        return ret;
    }

    //temporary method - remove if this doesn't work along with added fields
    //this method should return error is assigned effect is smaller than default
    @Override
    public Void visitVariable(VariableTree node, Void p)
    {
        Element methElt = TreeUtils.elementFromDeclaration(node);


        assert (methElt != null);

        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = atypeFactory.getDeclAnnotation(methElt, OkEffect);
/*
            (atypeFactory)
                    .checkEffectOverride(
                            (TypeElement) (methElt.getEnclosingElement()), methElt, true, node);
*/
            if (annotatedEffect == null) {
                atypeFactory
                        .fromElement(methElt)
                        .addAnnotation(atypeFactory.getDeclaredEffect(methElt, getClassTree()));
            }
        }

        currentVars.push(node);

        varEffStack.push(atypeFactory.getDeclaredEffect(methElt, getClassTree()));


        Void ret = super.visitVariable(node, p);
        currentVars.pop();
        varEffStack.pop();
        return ret;

    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        //TODO: Same effect checks as for methods
        return super.visitMemberSelect(node, p);
    }

    @Override
    public void processClassTree(ClassTree node) {
        // Fix up context for static initializers of new class
        currentMethods.push(null);
        effStack.push(genericEffect.getBottomMostEffectInLattice());
        super.processClassTree(node);
        currentMethods.pop();
        effStack.pop();
    }

    private Element getClassTree()
    {

        ClassTree clsTree = TreeUtils.enclosingClass(getCurrentPath());
        Element clsElt = TreeUtils.elementFromDeclaration(clsTree);
        return getTopLevelClass(clsElt);
        /*
        Element clsOuter = clsElt.getEnclosingElement();
        if(clsElt.getAnnotation(DefaultEffect.class) != null)
        {
            return clsElt;
        }
        while(clsOuter.getKind() == ElementKind.CLASS)
        {
            if(clsOuter.getAnnotation(DefaultEffect.class) != null) {
                return clsOuter;
            }
            else
            {
                clsOuter = clsOuter.getEnclosingElement();
            }
        }
        return clsElt;
        */
    }

    private Element getTopLevelClass(Element elt)
    {
        final Element firstElt = elt;
        if(elt == null)
        {
            return firstElt;
        }
        else if(elt.getAnnotation(DefaultEffect.class) != null)
        {
            return elt;
        }
        else
        {
            return getTopLevelClass(elt.getEnclosingElement());
        }


       /* Element currentElt = elt;
        while(currentElt.getEnclosingElement() != null)
        {
            if(elt.getAnnotation(DefaultEffect.class) != null) {
                return elt;
            }
            else {
                currentElt = elt.getEnclosingElement();
            }
        }
        return elt;
        final Element firstElt = elt;
        if(elt.getEnclosingElement() == null)
        {
            return firstElt;
        }
        else
        {
            if(elt.getAnnotation(DefaultEffect.class) != null) {
                //checker.userErrorAbort((elt.getEnclosingElement().getAnnotation(DefaultEffect.class) != null) + "");
                return elt;
            }
            return getTopLevelClass(elt.getEnclosingElement());
        }*/

    }


    /*
     * Method to check if the constructor call is made from a valid context
     */
    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        /*
        if (debugSpew) {
            System.err.println(
                    "For constructor " + node + " in " + currentMethods.peek().getName());
        }

        // Target method annotations
        ExecutableElement methodElt = TreeUtils.elementFromUse(node);
        if (debugSpew) {
            System.err.println("methodElt found");
        }

        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        if (callerTree == null) {
            if (debugSpew) {
                System.err.println("No enclosing method: likely static initializer");
            }
            return super.visitNewClass(node, p);
        }
        if (debugSpew) {
            System.err.println("callerTree found");
        }

        ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
        if (debugSpew) {
            System.err.println("callerElt found");
        }

        Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt);
        Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt);

        if (!genericEffect.LE(targetEffect, callerEffect)) {
            checker.report(
                    Result.failure("constructor.call.invalid", targetEffect, callerEffect), node);
            if (debugSpew) {
                System.err.println("Issuing error for node: " + node);
            }
        }
        if (debugSpew) {
            System.err.println(
                    "Successfully finished main non-recursive checkinv of invocation " + node);
        }

        return super.visitNewClass(node, p);
        */

        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        if(isWithinMethod(callerTree)) {
            //Class<? extends Annotation> targetEffect = extension.checkArrayAccess(node);
            ExecutableElement methodElt = TreeUtils.elementFromUse(node);
            Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt, getClassTree());

            checkWithinMethod(callerTree, node, targetEffect, "method.invalid");
            return super.visitNewClass(node, p);
        }
        VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
        if(isWithinVariable(varTree)) {
            //Class<? extends Annotation> varTargetEffect = extension.checkArrayAccess(node);
            ExecutableElement varElt = TreeUtils.elementFromUse(node);
            Class<? extends Annotation> varTargetEffect = atypeFactory.getDeclaredEffect(varElt, getClassTree());

            checkWithinVariable(varTree, node, varTargetEffect, "field.invalid");
            return super.visitNewClass(node, p);
        }
        return super.visitNewClass(node, p);
    }


    //this is temporary
    /*
    DataCapture d = new DataCapture("D:\\Research\\text.txt");
    @Override
    public Void visitIdentifier(IdentifierTree node, Void p)
    {
        // Target method annotations
        Element methodElt = TreeUtils.elementFromUse(node);

        MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
        if (callerTree == null) {
            return super.visitIdentifier(node, p);
        }

        ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);

        Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt);
        Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt);

        d.createData(targetEffect.toString()+" "+callerEffect.toString()+" "+node.toString());

        if (!genericEffect.LE(targetEffect, callerEffect)) {
            checker.report(
                    Result.failure("field.invalid", targetEffect, callerEffect), node);
        }

        VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
        if(isWithinVariable(varTree)) {
            Class<? extends Annotation> varTargetEffect = atypeFactory.getDeclaredEffect(methodElt);
            checkWithinVariable(varTree, node, varTargetEffect, "conditional.expression.invalid");
            return super.visitIdentifier(node, p);
        }

        return super.visitIdentifier(node, p);
    }
    */

    //clean up above

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        if(extension.doesArrayAccessCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkArrayAccess(node);
                checkWithinMethod(callerTree, node, targetEffect, "array.access.invalid");
                return super.visitArrayAccess(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkArrayAccess(node);
                checkWithinVariable(varTree, node, varTargetEffect, "array.access.invalid");
                return super.visitArrayAccess(node, p);
            }
            return super.visitArrayAccess(node, p);
        }
        return null;
    }

    //do I need a variable check for this
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        if(extension.doesCompoundAssignmentCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkCompoundAssignment(node);
                checkWithinMethod(callerTree, node, targetEffect, "compound.assignment.invalid");
                return super.visitCompoundAssignment(node, p);
            }
            /*
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkCompoundAssignment(node);
                checkWithinVariable(varTree, node, varTargetEffect, "compound.assignment.invalid");
                return super.visitCompoundAssignment(node, p);
            }
            */
            return super.visitCompoundAssignment(node, p);
        }
        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        if(extension.doesConditionalExpressionCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkConditionalExpression(node);
                checkWithinMethod(callerTree, node, targetEffect, "conditional.expression.invalid");
                return super.visitConditionalExpression(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkConditionalExpression(node);
                checkWithinVariable(varTree, node, varTargetEffect, "conditional.expression.invalid");
                return super.visitConditionalExpression(node, p);
            }
            return super.visitConditionalExpression(node, p);
        }
        return null;
    }
    //do i need a variable check for this?
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        if(extension.doesEnhancedForLoopCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkEnhancedForLoop(node);
                checkWithinMethod(callerTree, node, targetEffect, "enhanced.for.loop.invalid");
                return super.visitEnhancedForLoop(node, p);
            }
            /*
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkEnhancedForLoop(node);
                checkWithinVariable(varTree, node, varTargetEffect, "enhanced.for.loop.invalid");
                return super.visitEnhancedForLoop(node, p);
            }
            */
            return super.visitEnhancedForLoop(node, p);
        }
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {
        if(extension.doesInstanceOfCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkInstanceOf(node);
                checkWithinMethod(callerTree, node, targetEffect, "instance.of.invalid");
                return super.visitInstanceOf(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkInstanceOf(node);
                checkWithinVariable(varTree, node, varTargetEffect, "instance.of.invalid");
                return super.visitInstanceOf(node, p);
            }
            return super.visitInstanceOf(node, p);
        }
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        if(extension.doesNewArrayCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkNewArray(node);
                checkWithinMethod(callerTree, node, targetEffect, "new.array.invalid");
                return super.visitNewArray(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkNewArray(node);
                checkWithinVariable(varTree, node, varTargetEffect, "new.array.invalid");
                return super.visitNewArray(node, p);
            }
            return super.visitNewArray(node, p);
        }
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        if(extension.doesReturnCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkReturn(node);
                checkWithinMethod(callerTree, node, targetEffect, "return.invalid");
                return super.visitReturn(node, p);
            }
            /*
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkReturn(node);
                checkWithinVariable(varTree, node, varTargetEffect, "return.invalid");
                return super.visitReturn(node, p);
            }
            */
            return super.visitReturn(node, p);
        }
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        if(extension.doesUnaryCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkUnary(node);
                checkWithinMethod(callerTree, node, targetEffect, "unary.invalid");
                return super.visitUnary(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkUnary(node);
                checkWithinVariable(varTree, node, varTargetEffect, "unary.invalid");
                return super.visitUnary(node, p);
            }
            return super.visitUnary(node, p);
        }
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if(extension.doesTypeCastCheck()) {
            //ClassTree clsTree = TreeUtils.enclosingClass(getCurrentPath());


            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                Class<? extends Annotation> targetEffect = extension.checkTypeCast(node);


                ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
                Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt, getClassTree());
                checkWarning(node, targetEffect, callerEffect);


                checkWithinMethod(callerTree, node, targetEffect, "cast.invalid");
                return super.visitTypeCast(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                Class<? extends Annotation> varTargetEffect = extension.checkTypeCast(node);


                VariableElement varElt = TreeUtils.elementFromDeclaration(varTree);
                Class<? extends Annotation> varCallerEffect = atypeFactory.getDeclaredEffect(varElt, getClassTree());
                checkWarning(node, varTargetEffect, varCallerEffect);


                checkWithinVariable(varTree, node, varTargetEffect, "cast.invalid");
                return super.visitTypeCast(node, p);
            }
            return super.visitTypeCast(node, p);
        }
        return null;
    }

    private void checkWarning(Tree node, Class<? extends Annotation> target, Class<? extends Annotation> caller)
    {
        if(extension.reportWarning(node) != null)
            checker.report(Result.warning(extension.reportWarning(node), target, caller), node);
    }

    private void checkError(Tree node, Class<? extends Annotation> target, Class<? extends Annotation> caller)
    {
        //this needs to be fixed to show target and caller effects
        if(extension.reportError(node) != null)
            checker.report(Result.failure(extension.reportError(node), target, caller), node);
    }





/*
    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        if(extension.doesAssignmentCheck()) {
            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(isWithinMethod(callerTree)) {
                //Class<? extends Annotation> targetEffect = extension.checkAssignment(node);

                ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);

                Class<? extends Annotation> targetEffect = atypeFactory.getDeclaredEffect(methodElt);
                Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt);

                d.createData(targetEffect.toString()+" "+callerEffect.toString()+" "+node.toString());

                if (!genericEffect.LE(targetEffect, callerEffect)) {
                    checker.report(
                            Result.failure("field.invalid", targetEffect, callerEffect), node);
                }
                //checkWithinMethod(callerTree, node, targetEffect, "assignment.invalid");
                return super.visitAssignment(node, p);
            }
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());
            if(isWithinVariable(varTree)) {
                //Class<? extends Annotation> varTargetEffect = extension.checkAssignment(node);

                Element varElt = TreeUtils.elementFromDeclaration(varTree);

                Class<? extends Annotation> varTargetEffect = atypeFactory.getDeclaredEffect();
                Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(varElt);

                d.createData(varTargetEffect.toString()+" "+callerEffect.toString()+" "+node.toString());

                if (!genericEffect.LE(varTargetEffect, callerEffect)) {
                    checker.report(
                            Result.failure("field.invalid", varTargetEffect, callerEffect), node);
                }
                //checkWithinVariable(varTree, node, varTargetEffect, "assignment.invalid");
                return super.visitAssignment(node, p);
            }
            return super.visitAssignment(node, p);
        }
        return null;
    }
*/
    private boolean isWithinMethod(MethodTree callerTree) {
        return callerTree != null ? true : false;
    }
    //chance this to have enclosed in it
    private void checkWithinMethod(MethodTree callerTree, Tree node, Class<? extends Annotation> targetEffect, String errorMsg) {
        ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
        Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt, getClassTree());


        //checkWarning(node, targetEffect, callerEffect);


        if (checker.getOption("ignoreEffects") != null) {
            targetEffect = extension.checkIgnoredEffects(checker.getOption("ignoreEffects"), targetEffect);
        }
        if (!genericEffect.LE(targetEffect, callerEffect)) {
            //checkError(node, targetEffect, callerEffect);
            checker.report(
                    Result.failure(errorMsg, targetEffect, callerEffect), node);
        }
    }

    private boolean isWithinVariable(VariableTree variableTree) {
        return variableTree != null ? true : false;
    }
    //change this to have enclosed in it
    private void checkWithinVariable(VariableTree variableTree, Tree node, Class<? extends Annotation> targetEffect, String errorMsg) {
        VariableElement varElt = TreeUtils.elementFromDeclaration(variableTree);
        //make sure this makes sense
        Class<? extends Annotation> varCallerEffect = atypeFactory.getDeclaredEffect(varElt, getClassTree());


        //checkWarning(node, targetEffect, varCallerEffect);


        if (checker.getOption("ignoreEffects") != null) {
            targetEffect = extension.checkIgnoredEffects(checker.getOption("ignoreEffects"), targetEffect);
        }
        if (!genericEffect.LE(targetEffect, varCallerEffect)) {
            //checkError(node, targetEffect, varCallerEffect);
            checker.report(
                    Result.failure(errorMsg, targetEffect, varCallerEffect), node);
        }
    }





/*
    private void checkWithinClass(ClassTree clsTree, Tree node, Class<? extends Annotation> targetEffect, String errorMsg) {
        ClassElement varElt = TreeUtils.elementFromDeclaration();
        //make sure this makes sense
        Class<? extends Annotation> varCallerEffect = atypeFactory.getDeclaredEffect(varElt, getClassTree());
        if (checker.getOption("ignoreEffects") != null) {
            targetEffect = extension.checkIgnoredEffects(checker.getOption("ignoreEffects"), targetEffect);
        }
        if (!genericEffect.LE(targetEffect, varCallerEffect)) {
            checker.report(
                    Result.failure(errorMsg, targetEffect, varCallerEffect), node);
        }
    }
    */
}