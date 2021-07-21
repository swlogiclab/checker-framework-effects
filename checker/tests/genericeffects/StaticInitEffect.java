// @skip-test until support for commutative static initializers is added
import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.UnsafeDecimalCast;

@DefaultEffect(UnsafeDecimalCast.class)
public class StaticInitEffect {

  static int bar = 4234234;
  static byte foo = (byte)bar;

  static {
    int a = 1234;
    // :: error: (operation.invalid)
    byte b = (byte) a;

    double c = 123456;
    // okay
    short d = (short) c;

    // :: error: (operation.invalid)
    integerOverflow();
  }

  @IntegerOverflow
  public static void integerOverflow() {}
}
