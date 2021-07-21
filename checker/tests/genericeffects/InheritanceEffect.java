import org.checkerframework.checker.genericeffects.qual.DecimalOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerPrecisionLoss;

// :: error: (operation.invalid)
public class InheritanceEffect extends DummyClass {

  @DecimalOverflow
  public void decimalOverflow() {
    // :: error: (operation.invalid)
    integerPrecisionLoss();
  }

  @IntegerPrecisionLoss
  public void integerPrecisionLossTest() {
    // okay
    integerPrecisionLoss();
  }
}
