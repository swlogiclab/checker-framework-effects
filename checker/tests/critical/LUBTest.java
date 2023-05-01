import org.checkerframework.checker.critical.qual.*;

public class LUBTest {
    
    @Critical
    public void goodLUB1(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            critHelp.CritRegion();
        }
        else {
            critHelp.NotRelateLock();
        }
    }
    
    @Entrant
    public void goodLUB2(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            critHelp.EntAndLeaCrit();
        }
        else {
            critHelp.NotRelateLock();
        }
    }
    
    @Locking
    public void badLUB1(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            critHelp.Acquire();
        }
        else {
            critHelp.CritRegion();
        }
    }
    
    @Locking
    public void badLUB2(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            critHelp.EntAndLeaCrit();
        }
        else {
            critHelp.Acquire();
        }
    }
    
    @Unlocking
    public void badLUB3(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            critHelp.EntAndLeaCrit();
        }
        else {
            critHelp.CritRegion();
        }
    }
    
    @Unlocking
    public void badLUB4(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            critHelp.Acquire();
        }
        else {
            critHelp.Release();
        }
    }
}
