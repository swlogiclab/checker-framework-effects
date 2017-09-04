package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;

import java.lang.annotation.Annotation;

public class GenericEffectExtension {

    protected  GenericEffectLattice genericEffects;

    /**
     * Constructor to set the lattice.
     *
     * @param lattice The lattice of the checker.
     */
    public GenericEffectExtension(GenericEffectLattice lattice) {
        genericEffects = lattice;
    }

    /**
     * These methods should be overridden in a new class depending on the type of checker the developer is creating.
     * Note: Mostly all trees are checked except for ones that have been judged to be unhelpful or
     * encompass too many things.
     *
     * @return A boolean value representing whether a check should take place (true) or not (false).
     */
    public boolean doesArrayAccessCheck() {
        return false;
    }

    public boolean doesArrayTypeCheck() {
        return false;
    }

    public boolean doesAssertCheck() {
        return false;
    }

    public boolean doesAssignmentCheck() {
        return false;
    }

    public boolean doesBinaryCheck() {
        return false;
    }

    public boolean doesBreakCheck() {
        return false;
    }

    public boolean doesCaseCheck() {
        return false;
    }

    public boolean doesCatchCheck() {
        return false;
    }

    public boolean doesCompoundAssignmentCheck() {
        return false;
    }

    public boolean doesConditionalExpressionCheck() {
        return false;
    }

    public boolean doesContinueCheck() {
        return false;
    }

    public boolean doesDoWhileLoopCheck() {
        return false;
    }

    public boolean doesEnhancedForLoopCheck() {
        return false;
    }

    public boolean doesForLoopCheck() {
        return false;
    }

    public boolean doesIfCheck() {
        return false;
    }

    public boolean doesInstanceOfCheck() {
        return false;
    }

    public boolean doesIntersectionTypeCheck() {
        return false;
    }

    public boolean doesLabeledStatementCheck() {
        return false;
    }

    public boolean doesLiteralCheck() {
        return false;
    }

    public boolean doesNewArrayCheck() {
        return false;
    }

    public boolean doesPrimitiveTypeCheck() {
        return false;
    }

    public boolean doesReturnCheck() {
        return false;
    }

    public boolean doesSwitchCheck() {
        return false;
    }

    public boolean doesSynchronizedCheck() {
        return false;
    }

    public boolean doesThrowCheck() {
        return false;
    }

    public boolean doesTryCheck() {
        return false;
    }

    public boolean doesTypeCastCheck() {
        return false;
    }

    public boolean doesUnaryCheck() {
        return false;
    }

    public boolean doesUnionTypeCheck() {
        return false;
    }

    public boolean doesWhileLoopCheck() {
        return false;
    }

    public boolean doesWildcardCheck() {
        return false;
    }

    /**
     * These methods should be overridden in a new class depending on the type of checker the developer is creating.
     * Note: Mostly all trees are checked except for ones that have been judged to be unhelpful or
     * encompass too many things.
     *
     * @param node The specific tree node that the developer wants to check.
     * @return The effect of the specific tree node or throws an UnsupportedOperationException if not overridden.
     */
    public Class<? extends Annotation> checkArrayAccess(ArrayAccessTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkArrayType(ArrayTypeTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkAssert(AssertTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkAssignment(AssignmentTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkBinary(BinaryTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkBreak(BreakTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkCase(CaseTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkCatch(CatchTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkCompoundAssignment(CompoundAssignmentTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkConditionalExpression(ConditionalExpressionTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkContinue(ContinueTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkDoWhileLoop(DoWhileLoopTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkEnhancedForLoop(EnhancedForLoopTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkForLoop(ForLoopTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkIf(IfTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkInstanceOf(InstanceOfTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkIntersectionType(IntersectionTypeTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkLabeledStatement(LabeledStatementTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkLiteral(LiteralTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkNewArray(NewArrayTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkPrimitiveType(PrimitiveTypeTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkReturn(ReturnTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkSwitch(SwitchTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkSynchronized(SynchronizedTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkThrow(ThrowTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkTry(TryTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkTypeCast(TypeCastTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkUnary(UnaryTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkUnionType(UnionTypeTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkWhileLoop(WhileLoopTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkWildcard(WildcardTree node) {
        throw new UnsupportedOperationException();
    }


    /**
     * This method should be overridden in a new class to define errors that should occur during checking.
     *
     * @param node A tree node that represents all nodes that are being checked.
     * @return An error/failure message depending on the tree node.
     */
    public String reportError(Tree node) {
        return null;
    }

    /**
     * This method should be overridden in a new class to define warnings that should occur during checking.
     *
     * @param node A tree node that represents all nodes that are being checked.
     * @return A warning message depending on the tree node.
     */
    public String reportWarning(Tree node) {
        return null;
    }

    /**
     * This method should not be overridden unless compiler arguments need to be parsed differently.
     *
     * @param compilerArgs Arguments that were taken from the compiler.
     * @param anno Effect that needs to be checked to see if it is in the compiler arguments.
     * @return The bottom most effect in the lattice if the effect was in the compiler arguments or else the original effect is returned.
     */
    public Class<? extends Annotation> checkIgnoredEffects(String compilerArgs, Class<? extends Annotation> anno)
    {
        String[] parsedArgs = compilerArgs.split(",");
        for(String args : parsedArgs) {
            if(args.equals(anno.getSimpleName()))
                return genericEffects.getBottomMostEffectInLattice();
        }
        return anno;
    }

    /**
     * This method should not be overridden unless compiler arguments need to be parsed differently.
     *
     * @param compilerArgs Arguments that were taken from the compiler.
     * @param error Failure/Warning that needs to be checked to see if it was in the compiler arguments.
     * @return A boolean value if the value is in the compiler arguments (true) or not (false).
     */
    public boolean isIgnored(String compilerArgs, String error) {
        String[] parsedArgs = compilerArgs.split(",");
        for(String args : parsedArgs) {
            if(args.equals(error))
                return true;
        }
        return false;
    }
}
