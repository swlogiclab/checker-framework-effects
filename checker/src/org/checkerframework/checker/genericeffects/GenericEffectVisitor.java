package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Stack;
import javax.lang.model.element.*;

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

    public GenericEffectVisitor(BaseTypeChecker checker, GenericEffectExtension ext) {
        super(checker);
        assert (checker instanceof GenericEffectChecker);
        debugSpew = checker.getLintOption("debugSpew", false);

        effStack = new Stack<Class<? extends Annotation>>();
        currentMethods = new Stack<MethodTree>();

        //added this assignment
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
            AnnotatedExecutableType method, MethodInvocationTree node) {}

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
    }
    DataCapture d = new DataCapture("D:\\Research\\data-class.txt");
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

    /*
     * Method to check if the constructor call is made from a valid context
     */
    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
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
    }


    //this method needs to be updated to work for generic effects
    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if(extension.doesTypeCastCheck()){

            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if(callerTree != null) {

                ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);

                Class<? extends Annotation> targetEffect = extension.checkTypeCast(node);
                Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt);

                //change this to work for generic class types

                if(checker.getLintOption("IgnoreIntegerOverflow", false))
                {
                    if(targetEffect == IntegerOverflow.class)
                        targetEffect = SafeCast.class;
                }
                if(checker.getLintOption("IgnoreDecimalOverflow", false))
                {
                    if(targetEffect == DecimalOverflow.class)
                        targetEffect = SafeCast.class;
                }
                if(checker.getLintOption("IgnoreIntegerPrecisionLoss", false))
                {
                    if(targetEffect == IntegerPrecisionLoss.class)
                        targetEffect = SafeCast.class;
                }
                if(checker.getLintOption("IgnoreDecimalPrecisionLoss", false))
                {
                    if(targetEffect == DecimalPrecisionLoss.class)
                        targetEffect = SafeCast.class;
                }

                if (!genericEffect.LE(targetEffect, callerEffect)) {
                    checker.report(
                            Result.failure("cast.invalid", targetEffect, callerEffect), node);
                }
                return super.visitTypeCast(node, p);
            }


            //this checks variables after methods are checked
            VariableTree varTree = TreeUtils.enclosingVariable(getCurrentPath());

            if(varTree != null) {

                VariableElement varElt = TreeUtils.elementFromDeclaration(varTree);

                Class<? extends Annotation> varTargetEffect = extension.checkTypeCast(node);
                Class<? extends Annotation> varCallerEffect = atypeFactory.getDeclaredEffect(varElt);

                if (!genericEffect.LE(varTargetEffect, varCallerEffect)) {
                    checker.report(
                            Result.failure("cast.invalid", varTargetEffect, varCallerEffect), node);
                }
                return super.visitTypeCast(node, p);
            }

            return super.visitTypeCast(node, p);



        }
        return null;
    }


    //update this method to look like typecast
    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        if(extension.doesAssignmentCheck()) {
            if (debugSpew) {
                System.err.println(
                        "For casting " + node + " in " + currentMethods.peek().getName());
            }

            // Target method annotations
            if (debugSpew) {
                System.err.println("methodElt found");
            }

            MethodTree callerTree = TreeUtils.enclosingMethod(getCurrentPath());
            if (callerTree == null) {
                if (debugSpew) {
                    System.err.println("No enclosing method: likely static initializer");
                }
                return super.visitAssignment(node, p);
            }
            if (debugSpew) {
                System.err.println("callerTree found");
            }

            ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
            if (debugSpew) {
                System.err.println("callerElt found");
            }

            Class<? extends Annotation> targetEffect;
            targetEffect = extension.checkAssignment(node);                  

            Class<? extends Annotation> callerEffect = atypeFactory.getDeclaredEffect(callerElt);
            if (!genericEffect.LE(targetEffect, callerEffect)) {
                checker.report(
                        Result.failure("assignment.invalid", targetEffect, callerEffect), node);
                if (debugSpew) {
                    System.err.println("Issuing error for node: " + node);
                }
            }
            if (debugSpew) {
                System.err.println(
                        "Successfully finished main non-recursive checkinv of invocation " + node);
            }

            return super.visitAssignment(node, p);
        }
        return null;
    }
}
