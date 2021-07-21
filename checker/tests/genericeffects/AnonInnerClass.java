import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.SafeCast;

@DefaultEffect(IntegerOverflow.class)
public class AnonInnerClass {

  public void allocateAnonInner() {
    new DummyInterface() {
      @Override 
      public void test() {
        // okay
        byte a = (byte) 1234;
        // :: error: (operation.invalid)
        byte b = (byte) 1234f;
      }
    };
  }
}
