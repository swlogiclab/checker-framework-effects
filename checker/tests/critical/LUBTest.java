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
            // :: error: (undefined.residual)
            critHelp.CritRegion();
        }
    }
    
    @Locking
    public void badLUB2(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.join)
        if (b) {
            critHelp.EntAndLeaCrit();
        }
        else {
            critHelp.Acquire();
        }
    }
    
    // This method has a residual failure in one branch, and completion failure on the other
    @Unlocking
    // :: error: (subeffect.invalid.methodbody)
    public void badLUB3(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            // :: error: (undefined.residual)
            critHelp.EntAndLeaCrit();
        }
        else {
            critHelp.CritRegion();
        }
    }
    
    @Unlocking
    public void badLUB4(boolean b, CoreCriticalTest.CriticalTestHelper critHelp) {
        if (b) {
            // :: error: (undefined.residual)
            critHelp.Acquire();
        }
        else {
            critHelp.Release();
        }
    }
}
