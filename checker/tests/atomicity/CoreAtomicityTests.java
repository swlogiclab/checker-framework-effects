import org.checkerframework.checker.atomicity.qual.*;
import org.checkerframework.checker.genericeffects.qual.ThrownEffect;

/**
 * This class collects tests which are nominally for the Atomicity effect checker, but
 * in practice are really framework tests for the GenericEffectChecker.
 * 
 * The AtomicityEffectQuantale is rich enough to test most functionality of the framework
 * quite thoroughly, because the effect quantale is non-trivial (more than 2 effects, and
 * not totally-ordered) but still relatively small (only 5 effects) and residuated (to
 * check error reporting precision).
 * 
 * The only framework functionality that cannot be tested with the Atomicity effect system
 * is how the framework handles <i>partial</i> effect quantale operations.
 */
public class CoreAtomicityTests {
  public static class TestException extends Exception {}
  public static class TestException2 extends Exception {}

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
    // the branch logic should see the other branch marked impossible, and "ignore" it when
    // computing the overall conditional effect, so there should be only one error reported in this
    // method.
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
    // the branch logic should see the other branch marked impossible, and "ignore" it when
    // computing the overall conditional effect, so there should be only one error reported in this
    // method.
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
    // the branch logic should report errors in both branches, rather than for the overall
    // conditional.
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

  @Atomic
  public void badloop0(AtomicityTestHelper h) {
    // This is subtlely type-incorrect
    // The repeated portion is body|>update|>cond, since the condition may fail.
    // In this case, that result is non-atomic
    // :: error: (undefined.residual)
    for (int i = (h.DoNothingBool() ? 0 : 1);
        i < (h.LockBool() ? 10 : 10);
        i += (h.UnlockBool() ? 1 : 2)) {
      h.Lock();
      h.WellSync();
      h.Unlock();
    }
  }

  @Atomic
  public void badloop2(AtomicityTestHelper h) {
    // non-atomic because unlock then lock
    for (int i = h.UnlockBool() ? 0 : 1;
        // :: error: (undefined.residual)
        i < (h.LockBool() ? 10 : 10);
        i += (h.UnlockBool() ? 1 : 2)) {
      h.Lock();
      h.WellSync();
      h.Unlock();
    }
  }

  @Atomic
  public void badloop3(AtomicityTestHelper h) {
    // non-atomic because unlock then lock
    for (int i = h.DoNothingBool() ? 0 : 1;
        i < (h.LockBool() ? 10 : 10);
        i += (h.UnlockBool() ? 1 : 2)) {
      h.Lock();
      // :: error: (undefined.residual)
      h.DoStuff();
      h.Unlock();
    }
  }

  // Exception tests
  @Both
  @ThrownEffect(exception = Exception.class, behavior = Right.class)
  public void excTest0(AtomicityTestHelper h) throws Exception {
    h.Lock();
    throw new Exception();
  }

  @Atomic
  @ThrownEffect(exception = Exception.class, behavior = Right.class)
  public void excTest1(AtomicityTestHelper h) throws Exception {
    h.Lock();
    if (h.DoNothingBool()) {
      throw new Exception();
    } else {
      h.Unlock();
    }
  }

  // Throw an exception not known to the compiler, which requires the use of ClassType instead of
  // Class<?> internally
  @Both
  @ThrownEffect(exception = TestException.class, behavior = Right.class)
  public void excTest2(AtomicityTestHelper h) throws TestException {
    h.Lock();
    throw new TestException();
  }

  @Atomic
  @ThrownEffect(exception = TestException.class, behavior = Right.class)
  public void excTest3(AtomicityTestHelper h) throws TestException {
    h.Lock();
    if (h.DoNothingBool()) {
      throw new TestException();
    } else {
      h.Unlock();
    }
  }

  @Atomic
  @ThrownEffect(exception = TestException.class, behavior = Right.class)
  public void excTest3b(AtomicityTestHelper h) throws TestException {
    h.Lock();
    if (h.DoNothingBool()) {
      throw new TestException();
    }
    // This is the same as excTest3, but with the completion after the conditional rather than in the else branch
    h.Unlock();
  }

  @Atomic
  @ThrownEffect(exception = TestException.class, behavior = Right.class)
  public void excTest4(AtomicityTestHelper h) throws TestException {
    // Check that errors from bad throw prefixes are localized to throws
    h.Lock();
    if (h.DoNothingBool()) {
      h.WellSync();
      // :: error: (undefined.residual)
      throw new TestException();
    } else {
      h.Unlock();
    }
  }

  @Atomic
  @ThrownEffect(exception = Exception.class, behavior = Right.class)
  public void excTest5(AtomicityTestHelper h) throws TestException {
    h.Lock();
    if (h.DoNothingBool()) {
      // This is a *subtype* of the declared exception
      throw new TestException();
    } else {
      h.Unlock();
    }
  }

  @Atomic
  public void catchTest00(AtomicityTestHelper h) throws TestException {
    h.Lock();
    try {
      throw new TestException();
    } catch (TestException e) {
      h.Unlock();
    }
  }

  @Atomic
  public void catchTest01(AtomicityTestHelper h) throws TestException {
    h.Lock();
    try {
      h.Unlock();
      throw new TestException();
    } catch (TestException e) {
      // lock after unlock is non-atomic
      // :: error: (undefined.residual)
      h.Lock();
    }
  }

  @Atomic
  public void catchTest0(AtomicityTestHelper h) {
    h.Lock();
    try {
      if (h.WellSyncBool()) {
        throw new TestException();
      }
    } catch (TestException e) {
      h.Unlock();
    }
  }

  @Atomic
  public void catchTest1(AtomicityTestHelper h) {
    h.Lock();
    try {
      if (h.WellSyncBool()) {
        throw new TestException();
      }
      // Catch a supertype of the thrown exception
    } catch (Exception e) {
      h.Unlock();
    }
  }

  @Atomic
  public void catchTest2(AtomicityTestHelper h) {
    h.Lock();
    try {
      if (h.WellSyncBool()) {
        throw new TestException();
      }
      // Catch a supertype of the thrown exception
    } catch (Exception e) {
      // Do nothing; same as test above but completion of Atomic effect is now after the catch
    }
    h.Unlock();
  }

  @Atomic
  @ThrownEffect(exception = Exception.class, behavior = Atomic.class)
  public void rethrowTest0(AtomicityTestHelper h) throws Exception {
    h.Lock();
    try {
      if (h.WellSyncBool()) {
        throw new TestException();
      }
    } catch (TestException e) {
      h.Unlock();
      // rethrow while atomic
      throw new Exception(e);
    }
  }

  @Atomic
  @ThrownEffect(exception = Exception.class, behavior = NonAtomic.class)
  public void rethrowTest1(AtomicityTestHelper h) throws Exception {
    h.Lock();
    try {
      if (h.WellSyncBool()) {
        throw new TestException();
      }
    } catch (TestException e) {
      h.Unlock();
      h.DoStuff();
      // rethrow as non-atomic
      throw new Exception(e);
    }
    h.Unlock();
  }

  @Atomic
  @ThrownEffect(exception = Exception.class, behavior = Atomic.class)
  public void rethrowTest2(AtomicityTestHelper h) throws Exception {
    h.Lock();
    try {
      if (h.WellSyncBool()) {
        throw new TestException();
      }
    } catch (TestException e) {
      h.Unlock();
      // The residual check fails here, because exceptions also need to have atomic prefixes by the
      // annotation above
      // :: error: (undefined.residual)
      h.DoStuff();
      // rethrow as non-atomic, while regular exceptions are still atomic
      throw new Exception(e);
    }
    h.Unlock();
  }

  @Atomic
  @ThrownEffect(exception = Exception.class, behavior = NonAtomic.class)
  // :: error: (subeffect.invalid.methodbody)
  public void completionCheck0(AtomicityTestHelper h) throws Exception {
    // This line is okay in isolation because the method is marked as possibly throwing after
    // non-atomic, so this is a valid prefix of the required throw
    h.DoStuff();
  }

  @Atomic
  @ThrownEffect(exception = Exception.class, behavior = NonAtomic.class)
  public void completionCheck1(AtomicityTestHelper h) throws Exception {
    // This line is okay in isolation because the method is marked as possibly throwing after
    // non-atomic, so this is a valid prefix of the required throw
    h.DoStuff();
    throw new Exception();
  }

  @Atomic
  @ThrownEffect(exception=TestException.class, behavior=Atomic.class)
  public void finallyTest0(AtomicityTestHelper h) throws TestException {
   h.Lock();
   try {
     if (h.WellSyncBool()) {
       throw new TestException();
     }
   } finally {
     // This actually needs to be *appended* to the behavior, because finally acts as a catch+act+rethrow
     h.Unlock();
   }
  }

  @Atomic
  public void finallyTest1(AtomicityTestHelper h) throws TestException {
   h.Lock();
   try {
      throw new TestException();
   } catch (TestException e) {
    h.WellSync();
   } finally {
     // This actually needs to be *appended* to the behavior, because finally acts as a catch+act+rethrow
     h.Unlock();
   }
  }


  // TODO: Also need tests with both catch and finally, tests with multiple exceptions caught, tests with different exceptions thrown and some caught with others escaping through finally
  // TODO: Need systematic testing of behavior when an exception and a subtype thereof are both thrown, and either the subtype or supertype is caught (allowing the supertype to escape if the subtype is caught)

  @Atomic
  @ThrownEffect(exception = TestException.class, behavior = Both.class)
  @ThrownEffect(exception = TestException2.class, behavior = Right.class)
  public void multiThrow0(AtomicityTestHelper h) throws TestException, TestException2 {
    if (h.DoNothingBool()) {
      throw new TestException();
    }
    h.Lock();
    if (h.DoNothingBool()) {
      throw new TestException2();
    }
    h.Unlock();
  }

  @Atomic
  @ThrownEffect(exception = TestException2.class, behavior = Atomic.class)
  public void finallyTest2(AtomicityTestHelper h) throws TestException2 {
    h.Lock();
    try {
      if (h.DoNothingBool()) {
        // So TestException should reach the catch with a R prefix
        throw new TestException();
      }
      // regular exceptions should have an R prefix
      h.Lock();
      throw new TestException2();
    } catch (TestException e) {
      // Should reach here with a R prefix, "leave" with an A prefix going into the finally
      h.WellSync();
    } finally {
      // Should reach here with two effects of interest:
      // The base effect should be A, which is the R@TestException|>A=A from the throw+catch.
      // There should also be a an R@Exception which will become A@Exception
      h.Unlock();
    }
  }

}
