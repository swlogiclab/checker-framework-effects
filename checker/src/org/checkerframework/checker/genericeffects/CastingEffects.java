package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.qual.*;
import org.checkerframework.framework.source.SupportedLintOptions;

/**
 * Test Class to test FileIO Effect Checker inside Generic Effect Checker
 *
 * <p>Creates and checks relationship among the valid effects of FileIO Effect Checker
 */
//@SupportedLintOptions({"IgnoreIntegerOverflow"})
public final class CastingEffects implements GenericEffectLattice {

    /**
     * Method to check Less than equal to Effect
     *
     * @param left : Left Effect
     * @param right: Right Effect
     * @return boolean true : if bottom effect is left effect and is equal to NoFileEffect OR if top
     *     effect is right effect and is equal to ReadWriteFileEffect OR if left effect and right effect are
     *     the same
     *     <p>false : otherwise
     */
    @Override
    public boolean LE(Class<? extends Annotation> left, Class<? extends Annotation> right) {
        assert (left != null && right != null);


        if(right.equals(UnsafeCast.class))
            return true;
        else if(right.equals(NumberPrecisionLoss.class))
            return left.equals(IntegerPrecisionLoss.class) || left.equals(DecimalPrecisionLoss.class)
                    || left.equals(SafeCast.class) || left.equals(NumberPrecisionLoss.class);
        else if(right.equals(UnsafeIntegerCast.class))
            return left.equals(IntegerPrecisionLoss.class) || left.equals(IntegerOverflow.class)
                    || left.equals(SafeCast.class) || left.equals(UnsafeIntegerCast.class);
        else if(right.equals(UnsafeDecimalCast.class))
            return left.equals(DecimalPrecisionLoss.class) || left.equals(DecimalOverflow.class)
                    || left.equals(SafeCast.class) || left.equals(DecimalPrecisionLoss.class);
        else if(right.equals(NumberOverflow.class))
            return left.equals(IntegerOverflow.class) || left.equals(DecimalOverflow.class)
                    || left.equals(SafeCast.class) || left.equals(NumberOverflow.class);
        else if(right.equals(IntegerPrecisionLoss.class))
            return left.equals(SafeCast.class) || left.equals(IntegerPrecisionLoss.class);
        else if(right.equals(DecimalPrecisionLoss.class))
            return left.equals(SafeCast.class) || left.equals(DecimalPrecisionLoss.class);
        else if(right.equals(IntegerOverflow.class))
            return left.equals(SafeCast.class) || left.equals(IntegerOverflow.class);
        else if(right.equals(DecimalOverflow.class))
            return left.equals(SafeCast.class) || left.equals(DecimalOverflow.class);
        else if(right.equals(SafeCast.class))
            return left.equals(SafeCast.class);

        return false;
    }

    /**
     * Method to get minimum of (l, r)
     *
     * @param l : left effect
     * @param r : right effect
     * @return minimum(l,r)
     */
    @Override
    public Class<? extends Annotation> min(
            Class<? extends Annotation> l, Class<? extends Annotation> r) {
        if (LE(l, r)) {
            return l;
        } else {
            return r;
        }
    }

    /**
     * Get the collection of valid effects. For FileIO EFfect checker: Valid Effects:
     * NoFileEffect, ReadFileEffect, WriteFileEffect, and ReadWriteFileEffect
     */
    @Override
    public ArrayList<Class<? extends Annotation>> getValidEffects() {

        ArrayList<Class<? extends Annotation>> listOfEffects = new ArrayList<>();
        listOfEffects.add(SafeCast.class);
        listOfEffects.add(IntegerPrecisionLoss.class);
        listOfEffects.add(DecimalPrecisionLoss.class);
        listOfEffects.add(IntegerOverflow.class);
        listOfEffects.add(DecimalOverflow.class);
        listOfEffects.add(NumberPrecisionLoss.class);
        listOfEffects.add(UnsafeIntegerCast.class);
        listOfEffects.add(UnsafeDecimalCast.class);
        listOfEffects.add(NumberOverflow.class);
        listOfEffects.add(UnsafeCast.class);

        listOfEffects.add(DefaultEffect.class);

        return listOfEffects;
    }

    /**
     * Get the Top Most Effect of Lattice. For FileIO EFfect checker: Top Most Effect of Lattice:
     * ReadWriteFileEffect
     */
    @Override
    public Class<? extends Annotation> getTopMostEffectInLattice() {
        return UnsafeCast.class;
    }

    /**
     * Get the Bottom Most Effect of Lattice. For FileIO EFfect checker: Bottom Most Effect of Lattice:
     * NoFileEffect
     */
    @Override
    public Class<? extends Annotation> getBottomMostEffectInLattice() {
        return SafeCast.class;
    }


    @Override
    public Class<? extends Annotation> checkClassType(String effect)
    {
        for(Class<? extends Annotation> validEffect : getValidEffects())
        {
            if(effect.equals(validEffect.getSimpleName()))
                return validEffect;
        }
        return getBottomMostEffectInLattice();
    }
}
