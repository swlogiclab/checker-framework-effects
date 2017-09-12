import org.checkerframework.checker.genericeffects.qual.UnsafeCast;

public interface DummyInterface {
    @UnsafeCast
    void test();
}
