package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.qual.DecimalOverflow;
import org.checkerframework.checker.genericeffects.qual.DecimalPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.NumberOverflow;
import org.checkerframework.checker.genericeffects.qual.NumberPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.SafeCast;
import org.checkerframework.checker.genericeffects.qual.UnsafeCast;
import org.checkerframework.checker.genericeffects.qual.UnsafeDecimalCast;
import org.checkerframework.checker.genericeffects.qual.UnsafeIntegerCast;

/**
 * Class to set up lattice for Casting Effect Checker within Generic Effect Checker
 *
 * <p>Creates and checks relationship among the valid effects of Casting Effect Checker
 */
public final class CastingEffects
    extends FlowInsensitiveEffectLattice<Class<? extends Annotation>> {

  ArrayList<Class<? extends Annotation>> listOfEffects = new ArrayList<>();

  /** Constructor that will add the defined list of effects to an ArrayList */
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
   * @param right : Right Effect
   * @return boolean true : if bottom effect is on the left and the top effect is on the right, or
   *     if effects are equal
   *     <p>false : otherwise
   */
  @Override
  public boolean LE(Class<? extends Annotation> left, Class<? extends Annotation> right) {
    assert (left != null && right != null);

    if (right == UnsafeCast.class) return true;
    else if (right == NumberPrecisionLoss.class)
      return left == IntegerPrecisionLoss.class
          || left == DecimalPrecisionLoss.class
          || left == SafeCast.class
          || left == NumberPrecisionLoss.class;
    else if (right == UnsafeIntegerCast.class)
      return left == IntegerPrecisionLoss.class
          || left == IntegerOverflow.class
          || left == SafeCast.class
          || left == UnsafeIntegerCast.class;
    else if (right == UnsafeDecimalCast.class)
      return left == DecimalPrecisionLoss.class
          || left == DecimalOverflow.class
          || left == SafeCast.class
          || left == UnsafeDecimalCast.class;
    else if (right == NumberOverflow.class)
      return left == IntegerOverflow.class
          || left == DecimalOverflow.class
          || left == SafeCast.class
          || left == NumberOverflow.class;
    else if (right == IntegerPrecisionLoss.class)
      return left == SafeCast.class || left == IntegerPrecisionLoss.class;
    else if (right == DecimalPrecisionLoss.class)
      return left == SafeCast.class || left == DecimalPrecisionLoss.class;
    else if (right == IntegerOverflow.class)
      return left == SafeCast.class || left == IntegerOverflow.class;
    else if (right == DecimalOverflow.class)
      return left == SafeCast.class || left == DecimalOverflow.class;
    else if (right == SafeCast.class) return left == SafeCast.class;

    return false;
  }

  @Override
  public Class<? extends Annotation> LUB(
      Class<? extends Annotation> left, Class<? extends Annotation> right) {
    assert (left != null && right != null);

    boolean integer = false;
    boolean decimal = false;
    boolean overflow = false;
    boolean precisionloss = false;

    // Shortcut
    if (left == UnsafeCast.class || right == UnsafeCast.class) {
      return UnsafeCast.class;
    }

    if (left == NumberOverflow.class || right == NumberOverflow.class) {
      integer = true;
      decimal = true;
      overflow = true;
    }
    if (left == UnsafeIntegerCast.class || right == UnsafeIntegerCast.class) {
      integer = true;
      overflow = true;
      precisionloss = true;
    }
    if (left == UnsafeDecimalCast.class || right == UnsafeDecimalCast.class) {
      decimal = true;
      overflow = true;
      precisionloss = true;
    }
    if (left == NumberPrecisionLoss.class || right == NumberPrecisionLoss.class) {
      integer = true;
      decimal = true;
      precisionloss = true;
    }
    if (left == IntegerPrecisionLoss.class || right == IntegerPrecisionLoss.class) {
      integer = true;
      precisionloss = true;
    }
    if (left == DecimalPrecisionLoss.class || right == DecimalPrecisionLoss.class) {
      decimal = true;
      precisionloss = true;
    }
    if (left == IntegerOverflow.class || right == IntegerOverflow.class) {
      integer = true;
      overflow = true;
    }
    if (left == DecimalOverflow.class || right == DecimalOverflow.class) {
      decimal = true;
      overflow = true;
    }

    if (integer && decimal) {
      // Number || Unsafe
      if (overflow && precisionloss) {
        return UnsafeCast.class;
      } else if (overflow) {
        return NumberOverflow.class;
      } else if (precisionloss) {
        return NumberPrecisionLoss.class;
      } else {
        assert false
            : "Shouldn't have Integer and Decimal set without some kind of specific cast mistake";
      }
    } else if (integer) {
      // Integer
      if (overflow && precisionloss) {
        return UnsafeIntegerCast.class;
      } else if (overflow) {
        return IntegerOverflow.class;
      } else if (precisionloss) {
        return IntegerPrecisionLoss.class;
      } else {
        assert false : "Shouldn't have Integer set without some kind of specific cast mistake";
      }
    } else if (decimal) {
      // Decimal
      if (overflow && precisionloss) {
        return UnsafeDecimalCast.class;
      } else if (overflow) {
        return DecimalOverflow.class;
      } else if (precisionloss) {
        return DecimalPrecisionLoss.class;
      } else {
        assert false : "Shouldn't have Decimal set without some kind of specific cast mistake";
      }
    }
    assert (!overflow && !precisionloss)
        : "No specific numeric categories possible, so no specific errors should be possible"
            + " either";
    return SafeCast.class;
  }

  /**
   * Method that gets the valid list of effects.
   *
   * @return ArrayList containing the list of effects.
   */
  @Override
  public ArrayList<Class<? extends Annotation>> getValidEffects() {
    return listOfEffects;
  }

  /**
   * Method that gets the bottom most effect in the lattice as defined by the developer.
   *
   * @return The bottom most effect (SafeCast).
   */
  @Override
  public Class<? extends Annotation> bottomEffect() {
    return SafeCast.class;
  }
}
