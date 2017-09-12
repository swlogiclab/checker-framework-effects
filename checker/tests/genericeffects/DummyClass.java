import org.checkerframework.checker.genericeffects.qual.DecimalPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerPrecisionLoss;

public class DummyClass {

    @IntegerOverflow
    public DummyClass() {}

    @IntegerPrecisionLoss
    public void integerPrecisionLoss() {}

    @DecimalPrecisionLoss
    public void decimalPrecisionLoss() {}
}
