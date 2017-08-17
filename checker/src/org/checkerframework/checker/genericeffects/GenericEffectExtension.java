package org.checkerframework.checker.genericeffects;

/**
 * Created by rishi on 7/14/2017.
 */


import com.sun.source.tree.*;
import org.checkerframework.framework.source.SupportedLintOptions;

import java.lang.annotation.Annotation;

public class GenericEffectExtension {

    protected  GenericEffectLattice genericEffects;
    //this constructore may not be needed
    public GenericEffectExtension(GenericEffectLattice lattice)
    {
        genericEffects = lattice;
    }

    public boolean doesArrayAccessCheck() { return false; }
    public boolean doesAssignmentCheck() { return false; }
    public boolean doesCompoundAssignmentCheck() { return false; }
    public boolean doesConditionalExpressionCheck() { return false; }
    public boolean doesEnhancedForLoopCheck() { return false; }
    public boolean doesInstanceOfCheck() { return false; }
    public boolean doesLambdaExpressionCheck() { return false; }
    public boolean doesNewArrayCheck() { return false; }
    public boolean doesParameterizedTypeCheck() { return false; }
    public boolean doesReturnCheck() { return false; }
    public boolean doesTypeCastCheck() {
        return false;
    }
    public boolean doesUnaryCheck() { return false; }
    public boolean doesVariableCheck() { return false; }

    public Class<? extends Annotation> checkArrayAccess(ArrayAccessTree node) {
        return null;
    }
    public Class<? extends Annotation> checkAssignment(AssignmentTree node) {
        return null;
    }
    public Class<? extends Annotation> checkCompoundAssignment(CompoundAssignmentTree node) {
        return null;
    }
    public Class<? extends Annotation> checkConditionalExpression(ConditionalExpressionTree node) {
        return null;
    }
    public Class<? extends Annotation> checkEnhancedForLoop(EnhancedForLoopTree node) {
        return null;
    }
    public Class<? extends Annotation> checkInstanceOf(InstanceOfTree node) {
        return null;
    }
    public Class<? extends Annotation> checkLamdaExpression(LambdaExpressionTree node) {
        return null;
    }
    public Class<? extends Annotation> checkNewArray(NewArrayTree node) {
        return null;
    }
    public Class<? extends Annotation> checkParameterizedType(ParameterizedTypeTree node) {
        return null;
    }
    public Class<? extends Annotation> checkReturn(ReturnTree node) {
        return null;
    }
    public Class<? extends Annotation> checkTypeCast(TypeCastTree node) {
        return null;
    }
    public Class<? extends Annotation> checkUnary(UnaryTree node) {
        return null;
    }
    public Class<? extends Annotation> checkVariable(VariableTree node) {
        return null;
    }

    public Class<? extends Annotation> checkIgnoredEffects(String compilerArgs, Class<? extends Annotation> anno){return null;}

    /*
    //Void visitAnnotation(AnnotationTree node, Void p)
    Void visitArrayAccess(ArrayAccessTree node, Void p)
    Void visitAssignment(AssignmentTree node, Void p)
    //Void visitCatch(CatchTree node, Void p)
    //Void visitClass(ClassTree classTree, Void p)
    //Void visitCompilationUnit(CompilationUnitTree node, Void p)
    Void visitCompoundAssignment(CompoundAssignmentTree node, Void p)
    Void visitConditionalExpression(ConditionalExpressionTree node, Void p)
    Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p)
    //Void visitIdentifier(IdentifierTree node, Void p)
    Void visitInstanceOf(InstanceOfTree node, Void p)
    Void visitLambdaExpression(LambdaExpressionTree node, Void p)
    //Void visitMemberReference(MemberReferenceTree node, Void p)
    //Void visitMethod(MethodTree node, Void p)
    //Void visitMethodInvocation(MethodInvocationTree node, Void p)
    Void visitNewArray(NewArrayTree node, Void p)
    //Void visitNewClass(NewClassTree node, Void p)
    Void visitParameterizedType(ParameterizedTypeTree node, Void p)
    Void visitReturn(ReturnTree node, Void p)
    //Void visitThrow(ThrowTree node, Void p)
    Void visitTypeCast(TypeCastTree node, Void p)
    //Void visitTypeParameter(TypeParameterTree node, Void p)
    Void visitUnary(UnaryTree node, Void p)
    Void visitVariable(VariableTree node, Void p)
    */
}
