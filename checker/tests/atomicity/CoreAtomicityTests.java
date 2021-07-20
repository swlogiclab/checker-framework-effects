import org.checkerframework.checker.atomicity.qual.*;

public class CoreAtomicityTests {
  public static interface AtomicityTestHelper {
    @Right
    public void Lock();

    @Left
    public void Unlock();

    @Atomic
    public void WellSync();

    @Both
    public void DoNothing();

    @NonAtomic
    public void DoStuff();
  }

  public static class BasicSubEffectTests {
    public AtomicityTestHelper h;

    @Right
    public void bothSubRight() {
      h.DoNothing();
    }

    @Left
    public void bothSubLeft() {
      h.DoNothing();
    }

    @Atomic
    public void leftSubAtomic() {
      h.Unlock();
    }

    @Atomic
    public void rightSubAtomic() {
      h.Lock();
    }

    @NonAtomic
    public void atomicNot() {
      h.WellSync();
    }

    @NonAtomic
    public void bottomTop() {
      h.DoNothing();
    }
  }

  @Atomic
  public void somethingAtomic(AtomicityTestHelper h) {
    h.Lock();
    h.DoNothing();
    h.Lock();
    h.WellSync();
    h.DoNothing();
    h.Unlock();
    h.Unlock();
  }

  @Atomic
  public void failedAtomic1(AtomicityTestHelper h) {
    // :: error: (undefined.residual)
    h.DoStuff();
    h.Lock();
    h.DoNothing();
    h.Lock();
    h.WellSync();
    h.DoNothing();
    h.Unlock();
    h.Unlock();
  }

  @Atomic
  public void failedAtomic2(AtomicityTestHelper h) {
    h.WellSync();
    h.DoNothing();
    // :: error: (undefined.residual)
    h.WellSync();
  }

  @Atomic
  public void failedAtomic3(AtomicityTestHelper h) {
    h.Lock();
    h.WellSync();
    h.DoNothing();
    h.Unlock();
    // :: error: (undefined.residual)
    h.WellSync();
  }

  @Left
  public void somethingLeft(AtomicityTestHelper h) {
    h.DoNothing();
    h.Unlock();
    h.DoNothing();
    h.Unlock();
    h.DoNothing();
  }

  @Left
  public void failedLeft1(AtomicityTestHelper h) {
    // :: error: (undefined.residual)
    h.Lock();
  }

  @Left
  public void failedLeft2(AtomicityTestHelper h) {
    // :: error: (undefined.residual)
    h.WellSync();
  }

  @Left
  public void failedLeft3(AtomicityTestHelper h) {
    // :: error: (undefined.residual)
    h.DoStuff();
  }

  @Right
  public void failedRight1(AtomicityTestHelper h) {
    // :: error: (undefined.residual)
    h.Unlock();
  }

  @Right
  public void failedRight2(AtomicityTestHelper h) {
    // :: error: (undefined.residual)
    h.WellSync();
  }

  @Right
  public void failedRight3(AtomicityTestHelper h) {
    // :: error: (undefined.residual)
    h.DoStuff();
  }
}
