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

    @Right
    public boolean LockBool();

    @Left
    public boolean UnlockBool();

    @Atomic
    public boolean WellSyncBool();

    @Both
    public boolean DoNothingBool();

    @NonAtomic
    public boolean DoStuffBool();
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

  @NonAtomic
  public void goodLUB1(boolean b, AtomicityTestHelper h) {
    if (b) {
      h.Lock();
    } else {
      h.Unlock();
    }
  }

  @Atomic
  public void goodLUB2(boolean b, AtomicityTestHelper h) {
    if (b) {
      h.WellSync();
    } else {
      h.DoNothing();
    }
  }

  @Right
  public void badLUB1(boolean b, AtomicityTestHelper h) {
    // the branch logic should see the other branch marked impossible, and "ignore" it when computing the overall conditional effect, so there should be only one error reported in this method.
    if (b) {
      h.Lock();
    } else {
      // Should only see an error here
      // :: error: (undefined.residual)
      h.Unlock();
    }
  }

  @Right
  public void badLUB2(boolean b, AtomicityTestHelper h) {
    // the branch logic should see the other branch marked impossible, and "ignore" it when computing the overall conditional effect, so there should be only one error reported in this method.
    if (b) {
      // Should only see an error here
      // :: error: (undefined.residual)
      h.Unlock();
    } else {
      h.Lock();
    }
  }

  @Right
  public void badLUB3(boolean b, AtomicityTestHelper h) {
    // the branch logic should report errors in both branches, rather than for the overall conditional.
    if (b) {
      // :: error: (undefined.residual)
      h.Unlock();
    } else {
      // :: error: (undefined.residual)
      h.Unlock();
    }
  }

  @NonAtomic
  public void goodloop1(AtomicityTestHelper h) {
    while (h.DoNothingBool()) {
      h.Lock();
      h.WellSync();
      h.Unlock();
    }
  }
  @Atomic
  public void badloop1(AtomicityTestHelper h) {
    // Iterating this atomic loop is not atomic
    // :: error: (undefined.residual)
    while (h.DoNothingBool()) {
      h.Lock();
      h.WellSync();
      h.Unlock();
    }
  }
}
