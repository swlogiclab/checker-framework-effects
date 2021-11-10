import org.checkerframework.checker.critical.qual.*;

public class LoopTest {
    
    @Entrant
    public void goodLoop1(CoreCriticalTest.CriticalTestHelper critHelp) {
        while(critHelp.NotRelateLockBool()) {
            critHelp.Acquire();
            critHelp.CritRegion();
            critHelp.Release()
        }
    }
    
    @Critical
    public void goodLoop2(CoreCriticalTest.CriticalTestHelper critHelp) {
        while(critHelp.NotRelateLockBool()) {
            critHelp.Release();
            critHelp.EntAndLeaCrit();
            critHelp.Acquire();
        }
    }
    
    @Locking
    public void badLoop1(CoreCriticalTest.CriticalTestHelper critHelp) {
        while(critHelp.NotRelateLockBool()) {
            critHelp.EntAndLeaCrit();
            critHelp.Acquire();
            critHelp.CritRegion();
        }
    }
    
    @Unlocking
    public void badLoop2(CoreCriticalTest.CriticalTestHelper critHelp) {
        while(critHelp.NotRelateLockBool()) {
            critHelp.CritRegion();
            critHelp.Release();
            critHelp.EntAndLeaCrit();
        }
    }
    
    @Locking
    public void badLoop3(CoreCriticalTest.CriticalTestHelper critHelp) {
        // undefined because lock then entrant
        for(int i = (critHelp.AcquireBool() ? 0 : 1);
           i < (critHelp.EntAndLeaCrit() ? 10 : 10);
           i += (critHelp.AcquireBool() ? 1 : 2)) {
            critHelp.EntAndLeaCrit();
            critHelp.Acquire();
            critHelp.CritRegion();
        }
    }
}