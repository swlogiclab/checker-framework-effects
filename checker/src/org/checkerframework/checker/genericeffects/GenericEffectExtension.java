package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import java.lang.annotation.Annotation;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;

public class GenericEffectExtension {

    protected GenericEffectLattice genericEffects;

    /**
     * Constructor to set the lattice.
     *
     * @param lattice The lattice of the checker.
     */
    public GenericEffectExtension(GenericEffectLattice lattice) {
        genericEffects = lattice;
    }

    /**
     * These methods should be overridden in a new class depending on the type of checker the
     * developer is creating. Note: Mostly all trees that are subclasses of ExpressionTree can be or
     * are checked except for AnnotatedTypeTree, AnnotationTree, ErroneousTree, IdentifierTree, and
     * ParenthesizedTree. These are not checked because they have been judged to be unhelpful or
     * encompass too many things.
     *
     * @return A boolean value representing whether a check should take place (true) or not (false).
     */
    public boolean doesArrayAccessCheck() {
        return false;
    }

    public boolean doesAssignmentCheck() {
        return false;
    }

    public boolean doesBinaryCheck() {
        return false;
    }

    public boolean doesCompoundAssignmentCheck() {
        return false;
    }

    public boolean doesConditionalExpressionCheck() {
        return false;
    }

    public boolean doesInstanceOfCheck() {
        return false;
    }

    public boolean doesLiteralCheck() {
        return false;
    }

    public boolean doesNewArrayCheck() {
        return false;
    }

    public boolean doesTypeCastCheck() {
        return false;
    }

    public boolean doesUnaryCheck() {
        return false;
    }

    /**
     * These methods should be overridden in a new class depending on the type of checker the
     * developer is creating. Note: Mostly all trees that are subclasses of ExpressionTree can be or
     * are checked except for AnnotatedTypeTree, AnnotationTree, ErroneousTree, IdentifierTree, and
     * ParenthesizedTree. These are not checked because they have been judged to be unhelpful or
     * encompass too many things.
     *
     * @param node The specific tree node that the developer wants to check.
     * @return The effect of the specific tree node or throws an UnsupportedOperationException if
     *     not overridden.
     */
    public Class<? extends Annotation> checkArrayAccess(ArrayAccessTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkAssignment(AssignmentTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkBinary(BinaryTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkCompoundAssignment(CompoundAssignmentTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkConditionalExpression(ConditionalExpressionTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkInstanceOf(InstanceOfTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkLiteral(LiteralTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkNewArray(NewArrayTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkTypeCast(TypeCastTree node) {
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> checkUnary(UnaryTree node) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method should be overridden in a new class to define errors that should occur during
     * checking.
     *
     * @param node A tree node that represents all nodes that are being checked.
     * @return An error/failure message depending on the tree node.
     */
    public @CompilerMessageKey String reportError(Tree node) {
        return null;
    }

    /**
     * This method should be overridden in a new class to define warnings that should occur during
     * checking.
     *
     * @param node A tree node that represents all nodes that are being checked.
     * @return A warning message depending on the tree node.
     */
    public @CompilerMessageKey String reportWarning(Tree node) {
        return null;
    }

    /**
     * This method should not be overridden unless compiler arguments need to be parsed differently.
     *
     * @param compilerArgs Arguments that were taken from the compiler.
     * @param anno Effect that needs to be checked to see if it is in the compiler arguments.
     * @return The bottom most effect in the lattice if the effect was in the compiler arguments or
     *     else the original effect is returned.
     */
    public Class<? extends Annotation> checkIgnoredEffects(
            String compilerArgs, Class<? extends Annotation> anno) {
        String[] parsedArgs = compilerArgs.split(",");
        for (String args : parsedArgs) {
            if (args.equals(anno.getSimpleName()))
                return genericEffects.getBottomMostEffectInLattice();
        }
        return anno;
    }

    /**
     * This method should not be overridden unless compiler arguments need to be parsed differently.
     *
     * @param compilerArgs Arguments that were taken from the compiler.
     * @param error Failure/Warning that needs to be checked to see if it was in the compiler
     *     arguments.
     * @return A boolean value if the value is in the compiler arguments (true) or not (false).
     */
    public boolean isIgnored(String compilerArgs, String error) {
        String[] parsedArgs = compilerArgs.split(",");
        for (String args : parsedArgs) {
            if (args.equals(error)) return true;
        }
        return false;
    }
}
