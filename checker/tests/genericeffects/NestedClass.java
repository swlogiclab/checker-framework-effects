import org.checkerframework.checker.genericeffects.qual.DecimalPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;

@DefaultEffect(IntegerOverflow.class) public class NestedClass {
    public class Nest1 {
        @DecimalPrecisionLoss
        public void nest1Test() {

        }
        public class Nest2 {
            public void nest2Test() {
                //okay
                byte a = (byte) 1234;
                //:: error: (cast.invalid)
                byte b = (byte) 1234f;
                //:: error: (call.invalid.effect)
                nest1Test();
            }
        }
    }
}
