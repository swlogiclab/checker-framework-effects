import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.UnsafeIntegerCast;

@DefaultEffect(UnsafeIntegerCast.class)
public class FieldEffect {

    public int a = 1234;
    // okay
    byte b = (byte) a;

    public double c = 123456;
    // :: error: (cast.invalid)
    short d = (short) c;
}
