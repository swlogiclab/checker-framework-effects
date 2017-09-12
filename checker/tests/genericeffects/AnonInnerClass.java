import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;

@DefaultEffect(IntegerOverflow.class) public class AnonInnerClass {

    public void test() {
        new DummyInterface() {
            @Override
            public void test() {
                //okay
                byte a = (byte) 1234;
                //:: error: (cast.invalid)
                byte b = (byte) 1234f;
            }
        };
    }
}
