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

import org.checkerframework.checker.genericeffects.qual.*;

/**
 * Created by rishi on 7/14/2017.
 */
public class CastingEffectsExtension extends GenericEffectExtension{
    //this lattice is not really needed
    GenericEffectLattice genericEffects = super.genericEffects;
    DataCapture d = super.d;
    public CastingEffectsExtension(GenericEffectLattice lattice)
    {
        super(lattice);
    }

    @Override
    public boolean doesTypeCastCheck()
    {
        return true;
    }

    @Override
    public Class<? extends Annotation> checkTypeCast(TypeCastTree node) {
        //String castTo = InternalUtils.typeOf(node.getType()).toString();
        //String beingCast = InternalUtils.typeOf(node.getExpression()).toString();
        /*
        if(!isSafeLiteral(node))
        {
            //d.createData(castTo, beingCast);
            d.createData(node);
        }
        return genericEffects.getBottomMostEffectInLattice();
         */
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

    @Override
    public boolean doesAssignmentCheck()
    {
        return false;
    }

    @Override
    public Class<? extends Annotation> checkAssignment(AssignmentTree node)
    {
        Tree.Kind assign = node.getExpression().getKind();
        TypeKind var = TreeUtils.elementFromUse(node.getVariable()).asType().getKind();
        if (assign.equals(Tree.Kind.BOOLEAN_LITERAL) && var.equals(TypeKind.BOOLEAN)) {
            return genericEffects.getTopMostEffectInLattice();
        } else {
            return genericEffects.getBottomMostEffectInLattice();
        }
    }

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


}
