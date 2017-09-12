import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.SafeCast;
import org.checkerframework.checker.genericeffects.qual.UnsafeCast;

public class OverrideEffect extends DummyClass {

    @IntegerOverflow
    public OverrideEffect() {

    }

    @Override
    @SafeCast
    //okay
    public void integerPrecisionLoss() {

    }

    @Override
    @UnsafeCast
    //:: error: (override.effect.invalid)
    public void decimalPrecisionLoss() {

    }
}
