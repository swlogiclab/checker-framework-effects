import org.checkerframework.checker.genericeffects.qual.NumberOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.DecimalOverflow;
import org.checkerframework.checker.genericeffects.qual.DecimalPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.DefaultEffect;

public class InnerClassEffect {

    public void safeCast() {
        //:: error: (call.invalid.effect)
        integerOverflowEffect();
        //:: error: (call.invalid.effect)
        integerPrecisionLossEffect();
        //:: error: (call.invalid.effect)
        decimalOverflowEffect();
        //:: error: (call.invalid.effect)
        decimalPrecisionLossEffect();
    }

    @IntegerOverflow
    public void integerOverflowEffect() {

    }

    @IntegerPrecisionLoss
    public void integerPrecisionLossEffect() {

    }

    @DecimalOverflow
    public void decimalOverflowEffect() {

    }

    @DecimalPrecisionLoss
    public void decimalPrecisionLossEffect() {

    }

    @DefaultEffect(NumberOverflow.class) public class UnsafeClass {

        public void numberOverflow() {
            //okay
            integerOverflowEffect();
            //:: error: (call.invalid.effect)
            integerPrecisionLossEffect();
            //okay
            decimalOverflowEffect();
            //:: error: (call.invalid.effect)
            decimalPrecisionLossEffect();
        }

        @IntegerOverflow
        public void integerOverflowEffect() {

        }

        @IntegerPrecisionLoss
        public void integerPrecisionLossEffect() {

        }

        @DecimalOverflow
        public void decimalOverflowEffect() {

        }

        @DecimalPrecisionLoss
        public void decimalPrecisionLossEffect() {

        }
    }

}
