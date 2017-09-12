import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;

public class ConstructorEffect {

    @IntegerOverflow
    public ConstructorEffect() {
        //okay
        byte a = (byte) 1234;
        //:: error: (cast.invalid)
        byte b = (byte) 1234f;
    }

    public void safeCast() {
        //:: error: (constructor.call.invalid)
        new ConstructorEffect();
    }

    @IntegerOverflow
    public void integerOverflow() {
        //okay
        new ConstructorEffect();
    }


}
