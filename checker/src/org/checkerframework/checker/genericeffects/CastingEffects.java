package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.qual.*;

/**
 * Class to set up lattice for Casting Effect Checker within Generic Effect Checker
 *
 * <p>Creates and checks relationship among the valid effects of Casting Effect Checker
 */
public final class CastingEffects implements GenericEffectLattice {

    ArrayList<Class<? extends Annotation>> listOfEffects = new ArrayList<>();

    /**
     * Constructor that will add the defined list of effects to an ArrayList
     */
    public CastingEffects() {
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
    }

    /**
     * Method to check Less than equal to Effect
     *
     * @param left : Left Effect
     * @param right: Right Effect
     * @return boolean true : if bottom effect is on the left and the top effect is on the right, or if effects are equal
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
     * Method that gets the valid list of effects.
     *
     * @return ArrayList containing the list of effects.
     */
    @Override
    public ArrayList<Class<? extends Annotation>> getValidEffects() { return listOfEffects; }

    /**
     * Method that gets the top most effect in the lattice as defined by the developer.
     *
     * @return The top most effect (UnsafeCast).
     */
    @Override
    public Class<? extends Annotation> getTopMostEffectInLattice() {
        return UnsafeCast.class;
    }

    /**
     * Method that gets the bottom most effect in the lattice as defined by the developer.
     *
     * @return The bottom most effect (SafeCast).
     */
    @Override
    public Class<? extends Annotation> getBottomMostEffectInLattice() {
        return SafeCast.class;
    }
}
