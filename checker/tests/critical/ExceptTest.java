import org.checkerframework.checker.critical.qual.*;
import org.checkerframework.checker.genericeffects.qual.ThrownEffect;

public class ExceptTest {
    public static class TestException extends Exception {}
    
    @Basic
    @ThrownEffect(exception = Exception.class, behavior = Entrant.class)
    public void excTest0(CoreCriticalTest.CriticalTestHelper critHelp) throws Exception {
        critHelp.EntAndLeaCrit();
        throw new Exception();
    }
    
    @Locking
    @ThrownEffect(exception = Exception.class, behavior = Entrant.class)
    public void excTest1(CoreCriticalTest.CriticalTestHelper critHelp) throws Exception {
        critHelp.EntAndLeaCrit();
        if (critHelp.NotRelateLockBool()) {
            throw new Exception();
        } else {
            critHelp.Acquire();
        }
    }
    
    @Basic
    @ThrownEffect(exception = TestException.class, behavior = Entrant.class)
    public void excTest2(CoreCriticalTest.CriticalTestHelper critHelp) throws TestException {
        critHelp.EntAndLeaCrit();
        throw new TestException();
    }
    
    @Locking
    @ThrownEffect(exception = TestException.class, behavior = Entrant.class)
    public void excTest3(CoreCriticalTest.CriticalTestHelper critHelp) throws TestException {
        critHelp.EntAndLeaCrit();
        if (critHelp.NotRelateLockBool()) {
            throw new TestException();
        } else {
            critHelp.Acquire();
        }
    }
    
    @Locking
    @ThrownEffect(exception = TestException.class, behavior = Entrant.class)
    public void excTest4(CoreCriticalTest.CriticalTestHelper critHelp) throws TestException {
        critHelp.EntAndLeaCrit();
        if (critHelp.NotRelateLockBool()) {
            critHelp.Acquire();
            throw new TestException();
        } else {
            critHelp.Acquire();
        }
    }
    
    @Locking
    @ThrownEffect(exception = Exception.class, behavior = Entrant.class)
    public void excTest5(CoreCriticalTest.CriticalTestHelper critHelp) throws TestException {
        critHelp.EntAndLeaCrit();
        if (critHelp.NotRelateLockBool()) {
            throw new TestException();
        } else {
            critHelp.Acquire();
        }
    }
    
    @Locking
    public void catchTest00(CoreCriticalTest.CriticalTestHelper critHelp) throws TestException {
        critHelp.EntAndLeaCrit();
        try {
            throw new TestException();
        } catch (TestException e) {
            critHelp.Acquire();
        }
    }
    
    @Locking
    public void catchTest01(CoreCriticalTest.CriticalTestHelper critHelp) throws TestException {
        critHelp.EntAndLeaCrit();
        try {
            critHelp.Acquire();
            throw new TestException();
        } catch (TestException e) {
            // Entrant after locking is undefined. 
            critHelp.EntAndLeaCrit();
        }
    }
    
    @Locking
    public void catchTest0(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.EntAndLeaCrit();
        try {
            if(critHelp.AcquireBool()) {
                throw new TestException();
            }
        } catch (TestException e) {
            critHelp.Acquire();
        }
    }
    
    @Locking
    public void catchTest1(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.EntAndLeaCrit();
        try {
            if (critHelp.AcquireBool()) {
                throw new TestException();
            }
            // Catch a supertype of the thrown exception
        } catch (Exception e) {
            critHelp.Acquire();
        }
    }
    
    @Locking
    @ThrownEffect(exception = Exception.class, behavior = Locking.class) 
    public void rethrowTest0(CoreCriticalTest.CriticalTestHelper critHelp) throws Exception {
        critHelp.EntAndLeaCrit();
        try {
            if (critHelp.AcquireBool()) {
                throw new TestException(); 
            }
        } catch (TestException e) {
            critHelp.Acquire();
            throw new Exception(e);
        }
    }
    
    @Locking
    @ThrownEffect(exception = Exception.class, behavior = Entrant.class)
    public void rethrowTest1(CoreCriticalTest.CriticalTestHelper critHelp) throws Exception {
        critHelp.EntAndLeaCrit();
        try {
            if (critHelp.AcquireBool()) {
                throw new TestException();
            }
        } catch (TestException e) {
            critHelp.Acquire();
            critHelp.Release(); //After unlocking is Entrant
            throw new Exception(e); // rethrow as entrant
        }
        critHelp.Acquire();
    }
    
    @Locking
    @ThrownEffect(exception = Exception.class, behavior = Locking.class)
    public void rethrowTest2(CoreCriticalTest.CriticalTestHelper critHelp) throws Exception {
        critHelp.EntAndLeaCrit();
        try {
            if (critHelp.AcquireBool()) {
                throw new TestException();
            }
        } catch (TestException e) {
            critHelp.Acquire();
            // :: error: (undefined.residual) 
            // exceptions need to have locking prefixes.
            critHelp.Release(); 
            throw new Exception(e);
        }
    }
    
    @Locking
    @ThrownEffect(exception = Exception.class, behavior = Unlocking.class)
    // exceptions need to have locking prefixes.
    public void completionCheck0(CoreCriticalTest.CriticalTestHelper critHelp) throws Exception {
        critHelp.Release(); 
    }
    
    @Locking
    @ThrownEffect(exception = Exception.class, behavior = Unlocking.class)
    public void completionCheck1(CoreCriticalTest.CriticalTestHelper critHelp) throws Exception {
        critHelp.Release();
        throw new Exception();
    }
    
}
    