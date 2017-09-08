import org.checkerframework.checker.genericeffects.qual.*;

public class CallEffect {

    @UnsafeCast
    public void unsafeEffect() {
        //okay
        unsafeEffect();
        //okay
        numberOverflowEffect();
        //okay
        numberPrecisionLossEffect();
        //okay
        unsafeIntegerCast();
        //okay
        unsafeDecimalCast();
        //okay
        integerOverflowEffect();
        //okay
        integerPrecisionLossEffect();
        //okay
        decimalOverflowEffect();
        //okay
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @NumberOverflow
    public void numberOverflowEffect() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //okay
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //okay
        integerOverflowEffect();
        //:: error: (call.invalid.effect)
        integerPrecisionLossEffect();
        //okay
        decimalOverflowEffect();
        //:: error: (call.invalid.effect)
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @NumberPrecisionLoss
    public void numberPrecisionLossEffect() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //okay
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //:: error: (call.invalid.effect)
        integerOverflowEffect();
        //okay
        integerPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        decimalOverflowEffect();
        //okay
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @UnsafeIntegerCast
    public void unsafeIntegerCast() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //okay
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //okay
        integerOverflowEffect();
        //okay
        integerPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        decimalOverflowEffect();
        //:: error: (call.invalid.effect)
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @UnsafeDecimalCast
    public void unsafeDecimalCast() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //okay
        unsafeDecimalCast();
        //:: error: (call.invalid.effect)
        integerOverflowEffect();
        //:: error: (call.invalid.effect)
        integerPrecisionLossEffect();
        //okay
        decimalOverflowEffect();
        //okay
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @IntegerOverflow
    public void integerOverflowEffect() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //okay
        integerOverflowEffect();
        //:: error: (call.invalid.effect)
        integerPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        decimalOverflowEffect();
        //:: error: (call.invalid.effect)
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }


    @IntegerPrecisionLoss
    public void integerPrecisionLossEffect() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //:: error: (call.invalid.effect)
        integerOverflowEffect();
        //okay
        integerPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        decimalOverflowEffect();
        //:: error: (call.invalid.effect)
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @DecimalOverflow
    public void decimalOverflowEffect() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //:: error: (call.invalid.effect)
        integerOverflowEffect();
        //:: error: (call.invalid.effect)
        integerPrecisionLossEffect();
        //okay
        decimalOverflowEffect();
        //:: error: (call.invalid.effect)
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @DecimalPrecisionLoss
    public void decimalPrecisionLossEffect() {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //:: error: (call.invalid.effect)
        integerOverflowEffect();
        //:: error: (call.invalid.effect)
        integerPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        decimalOverflowEffect();
        //okay
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }

    @SafeCast
    public void safeCastEffect()
    {
        //:: error: (call.invalid.effect)
        unsafeEffect();
        //:: error: (call.invalid.effect)
        numberOverflowEffect();
        //:: error: (call.invalid.effect)
        numberPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        unsafeIntegerCast();
        //:: error: (call.invalid.effect)
        unsafeDecimalCast();
        //:: error: (call.invalid.effect)
        integerOverflowEffect();
        //:: error: (call.invalid.effect)
        integerPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        decimalOverflowEffect();
        //:: error: (call.invalid.effect)
        decimalPrecisionLossEffect();
        //okay
        safeCastEffect();
    }
}
