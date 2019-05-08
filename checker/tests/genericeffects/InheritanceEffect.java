import org.checkerframework.checker.genericeffects.qual.DecimalOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerPrecisionLoss;

// :: error: (call.invalid.effect)
public class InheritanceEffect extends DummyClass {

    @DecimalOverflow
    public void decimalOverflow() {
        // :: error: (call.invalid.effect)
        integerPrecisionLoss();
    }

    @IntegerPrecisionLoss
    public void integerPrecisionLossTest() {
        // okay
        integerPrecisionLoss();
    }
}
