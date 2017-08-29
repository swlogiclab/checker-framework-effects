package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.xml.crypto.Data;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;

import org.checkerframework.checker.genericeffects.qual.*;

/**
 * Created by rishi on 7/14/2017.
 */
public class CastingEffectsExtension extends GenericEffectExtension{

    GenericEffectLattice genericEffects = super.genericEffects;

    /**
     * Constructor that takes the lattice in as a parameter and passes it to the constructor of the superclass.
     *
     * @param lattice The effect lattice for the checker.
     */
    public CastingEffectsExtension(GenericEffectLattice lattice) {
        super(lattice);
    }

    /**
     * Overridden method from GenericEffectExtension that activates type cast checking.
     *
     * @return A boolean value representing whether the type cast check is activated.
     */
    @Override
    public boolean doesTypeCastCheck()
    {
        return true;
    }

    /**
     * Overridden method from GenericEffectExtension that specifies the effects of type cast tree nodes.
     *
     * @param node A type cast tree node that is encountered when checking the program.
     * @return The effect of node that is encountered.
     */
    @Override
    public Class<? extends Annotation> checkTypeCast(TypeCastTree node) {
        TypeKind castTo = InternalUtils.typeOf(node.getType()).getKind();
        TypeKind beingCast = InternalUtils.typeOf(node.getExpression()).getKind();
        if(isSafeLiteral(node))
            return SafeCast.class;
        else if ((beingCast.equals(TypeKind.LONG) && (castTo.equals(TypeKind.INT) || castTo.equals(TypeKind.SHORT) || castTo.equals(TypeKind.BYTE)))
                || (beingCast.equals(TypeKind.INT) && (castTo.equals(TypeKind.SHORT) || castTo.equals(TypeKind.BYTE)))
                || (beingCast.equals(TypeKind.SHORT) && castTo.equals(TypeKind.BYTE)))
            return IntegerOverflow.class;
        else if((beingCast.equals(TypeKind.DOUBLE) || beingCast.equals(TypeKind.FLOAT))
                && (castTo.equals(TypeKind.LONG) || castTo.equals(TypeKind.INT) || castTo.equals(TypeKind.SHORT) || castTo.equals(TypeKind.BYTE)))
            return DecimalOverflow.class;
        else if((beingCast.equals(TypeKind.LONG) && (castTo.equals(TypeKind.DOUBLE) || castTo.equals(TypeKind.FLOAT)))
                || (beingCast.equals(TypeKind.INT) && castTo.equals(TypeKind.FLOAT)))
            return IntegerPrecisionLoss.class;
        else if(beingCast.equals(TypeKind.DOUBLE) && castTo.equals(TypeKind.FLOAT))
            return DecimalPrecisionLoss.class;
        else
            return SafeCast.class;

    }

    /**
     * Private method to for checking if a type cast involving a literal is safe.
     *
     * @param node A type cast tree node that is encountered while checking.
     * @return A boolean value representing whether the type cast tree node is safe for a casting involving literals.
     */
    private boolean isSafeLiteral(TypeCastTree node) {
        TypeMirror t = InternalUtils.typeOf(node.getType());
        if (node.getExpression().getKind().equals(Tree.Kind.INT_LITERAL)) {
            int val = Integer.parseInt(node.getExpression().toString());
            if (t.getKind().equals(TypeKind.BYTE)) {
                if (val > Byte.MAX_VALUE || val < Byte.MIN_VALUE)
                    return false;
            } else if (t.getKind().equals(TypeKind.SHORT)) {
                if (val > Short.MAX_VALUE || val < Short.MIN_VALUE)
                    return false;
            }
            return true;
        }
        return false;
    }


    /**
     * Overridden method that specifies which error to report depending on the nodes encountered while checking.
     * Note: This method does not take in a specific tree node; therefore, the tree node must be cast to a specific
     * tree node depending on which checks are active.
     *
     * @param node Nodes encountered for all active checks.
     * @return String object representing the failure message.
     */
    @Override
    public String reportError(Tree node)
    {
        if(node.getKind().equals(Tree.Kind.TYPE_CAST)) {
            TypeCastTree typeCastNode = (TypeCastTree) node;
            TypeKind castTo = InternalUtils.typeOf(typeCastNode.getType()).getKind();
            TypeKind beingCast = InternalUtils.typeOf(typeCastNode.getExpression()).getKind();
            if (isSafeLiteral(typeCastNode))
                return null;
            else if ((beingCast.equals(TypeKind.LONG) && (castTo.equals(TypeKind.INT) || castTo.equals(TypeKind.SHORT) || castTo.equals(TypeKind.BYTE)))
                    || (beingCast.equals(TypeKind.INT) && (castTo.equals(TypeKind.SHORT) || castTo.equals(TypeKind.BYTE)))
                    || (beingCast.equals(TypeKind.SHORT) && castTo.equals(TypeKind.BYTE)))
                return "cast.invalid";
            else if ((beingCast.equals(TypeKind.DOUBLE) || beingCast.equals(TypeKind.FLOAT))
                    && (castTo.equals(TypeKind.LONG) || castTo.equals(TypeKind.INT) || castTo.equals(TypeKind.SHORT) || castTo.equals(TypeKind.BYTE)))
                return "cast.invalid";
            else if ((beingCast.equals(TypeKind.LONG) && (castTo.equals(TypeKind.DOUBLE) || castTo.equals(TypeKind.FLOAT)))
                    || (beingCast.equals(TypeKind.INT) && castTo.equals(TypeKind.FLOAT)))
                return "cast.invalid";
            else if (beingCast.equals(TypeKind.DOUBLE) && castTo.equals(TypeKind.FLOAT))
                return "cast.invalid";
        }
        return null;
    }

    /**
     * Overridden method that specifies which warning to report depending on the nodes encountered while checking.
     * Note: This method does not take in a specific tree node; therefore, the tree node must be cast to a specific
     * tree node depending on which checks are active.
     *
     * @param node Nodes encountered for all active checks.
     * @return String object representing the warning message.
     */
    @Override
    public String reportWarning(Tree node)
    {
        if(node.getKind().equals(Tree.Kind.TYPE_CAST)) {
            TypeCastTree typeCastNode = (TypeCastTree) node;
            TypeKind castTo = InternalUtils.typeOf(typeCastNode.getType()).getKind();
            TypeKind beingCast = InternalUtils.typeOf(typeCastNode.getExpression()).getKind();
            if (beingCast.equals(castTo))
                return "cast.redundant";
        }
        return null;
    }
}
