import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.UnsafeDecimalCast;

@DefaultEffect(UnsafeDecimalCast.class) public class StaticInitEffect {
    static {
        int a = 1234;
        //:: error: (cast.invalid)
        byte b = (byte) a;

        double c = 123456;
        //okay
        short d = (short) c;

        //:: error: (call.invalid.effect)
        integerOverflow();
    }

    @IntegerOverflow
    public static void integerOverflow() {

    }
}
