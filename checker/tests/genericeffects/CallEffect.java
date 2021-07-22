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

public class CallEffect {

  @UnsafeCast
  public void unsafeEffect() {
    // okay
    unsafeEffect();
    // okay
    numberOverflowEffect();
    // okay
    numberPrecisionLossEffect();
    // okay
    unsafeIntegerCast();
    // okay
    unsafeDecimalCast();
    // okay
    integerOverflowEffect();
    // okay
    integerPrecisionLossEffect();
    // okay
    decimalOverflowEffect();
    // okay
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @NumberOverflow
  public void numberOverflowEffect() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // okay
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // okay
    integerOverflowEffect();
    // :: error: (operation.invalid)
    integerPrecisionLossEffect();
    // okay
    decimalOverflowEffect();
    // :: error: (operation.invalid)
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @NumberPrecisionLoss
  public void numberPrecisionLossEffect() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // okay
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // :: error: (operation.invalid)
    integerOverflowEffect();
    // okay
    integerPrecisionLossEffect();
    // :: error: (operation.invalid)
    decimalOverflowEffect();
    // okay
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @UnsafeIntegerCast
  public void unsafeIntegerCast() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // okay
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // okay
    integerOverflowEffect();
    // okay
    integerPrecisionLossEffect();
    // :: error: (operation.invalid)
    decimalOverflowEffect();
    // :: error: (operation.invalid)
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @UnsafeDecimalCast
  public void unsafeDecimalCast() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // okay
    unsafeDecimalCast();
    // :: error: (operation.invalid)
    integerOverflowEffect();
    // :: error: (operation.invalid)
    integerPrecisionLossEffect();
    // okay
    decimalOverflowEffect();
    // okay
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @IntegerOverflow
  public void integerOverflowEffect() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // okay
    integerOverflowEffect();
    // :: error: (operation.invalid)
    integerPrecisionLossEffect();
    // :: error: (operation.invalid)
    decimalOverflowEffect();
    // :: error: (operation.invalid)
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @IntegerPrecisionLoss
  public void integerPrecisionLossEffect() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // :: error: (operation.invalid)
    integerOverflowEffect();
    // okay
    integerPrecisionLossEffect();
    // :: error: (operation.invalid)
    decimalOverflowEffect();
    // :: error: (operation.invalid)
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @DecimalOverflow
  public void decimalOverflowEffect() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // :: error: (operation.invalid)
    integerOverflowEffect();
    // :: error: (operation.invalid)
    integerPrecisionLossEffect();
    // okay
    decimalOverflowEffect();
    // :: error: (operation.invalid)
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @DecimalPrecisionLoss
  public void decimalPrecisionLossEffect() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // :: error: (operation.invalid)
    integerOverflowEffect();
    // :: error: (operation.invalid)
    integerPrecisionLossEffect();
    // :: error: (operation.invalid)
    decimalOverflowEffect();
    // okay
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }

  @SafeCast
  public void safeCastEffect() {
    // :: error: (operation.invalid)
    unsafeEffect();
    // :: error: (operation.invalid)
    numberOverflowEffect();
    // :: error: (operation.invalid)
    numberPrecisionLossEffect();
    // :: error: (operation.invalid)
    unsafeIntegerCast();
    // :: error: (operation.invalid)
    unsafeDecimalCast();
    // :: error: (operation.invalid)
    integerOverflowEffect();
    // :: error: (operation.invalid)
    integerPrecisionLossEffect();
    // :: error: (operation.invalid)
    decimalOverflowEffect();
    // :: error: (operation.invalid)
    decimalPrecisionLossEffect();
    // okay
    safeCastEffect();
  }
}
