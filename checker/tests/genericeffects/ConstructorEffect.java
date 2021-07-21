import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;

public class ConstructorEffect {

  @IntegerOverflow
  public ConstructorEffect() {
    // okay
    byte a = (byte) 1234;
    // :: error: (operation.invalid)
    byte b = (byte) 1234f;
  }

  public void safeCast() {
    // :: error: (operation.invalid)
    new ConstructorEffect();
  }

  @IntegerOverflow
  public void integerOverflow() {
    // okay
    new ConstructorEffect();
  }
}
