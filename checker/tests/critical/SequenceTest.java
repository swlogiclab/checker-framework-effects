import org.checkerframework.checker.critical.qual.*;

public class SequenceTest {
    
    @Critical
    public void somethingCritical(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.CritRegion();
        critHelp.Release();
        critHelp.NotRelateLock();
        critHelp.EntAndLeaCrit();
        critHelp.EntAndLeaCrit();
        critHelp.Acquire();
    }
    
    @Critical
    public void somethingCritical1(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.NotRelateLock();
        critHelp.CritRegion();
        critHelp.Release();
        critHelp.Acquire();
    }
    
    @Critical
    public void failedCritical2(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.Acquire();
        critHelp.CritRegion();
    }
    
    @Critical
    public void failedCritical3(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.EntAndLeaCrit();
        critHelp.NotRelateLock();
        critHelp.Acquire();
    }
    
    @Critical
    public void failedCritical4(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.Acquire();
    }
    
    @Critical
    public void failedCritical5(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.EntAndLeaCrit();
    }
    
    @Entrant
    public void somethingEntrant(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.NotRelateLock();
        critHelp.Acquire();
        critHelp.Release();
    }
    
    @Entrant
    public void somethingEntrant1(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.Acquire();
        critHelp.CritRegion();
        critHelp.Release();
        critHelp.NotRelateLock();
        critHelp.EntAndLeaCrit();
    }
    
    @Entrant
    public void failedEntrant2(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.EntAndLeaCrit();
        critHelp.NotRelateLock();
        critHelp.EntAndLeaCrit();
        // :: error: (undefined.sequencing)
        critHelp.Release();
    }
    
    @Entrant
    public void failedEntrant3(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.Acquire();
        critHelp.CritRegion();
        // :: error: (undefined.sequencing)
        critHelp.EntAndLeaCrit();
    }
    
    @Entrant
    public void failedEntrant4(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.Release();
    }
    
    @Entrant
    public void failedEntrant5(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.CritRegion();
    }
    
    @Locking
    public void somethingLock(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.NotRelateLock();
        critHelp.EntAndLeaCrit();
        critHelp.EntAndLeaCrit();
        critHelp.Acquire();
    }
    
    @Locking
    public void somethingLock1(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.Acquire();
        critHelp.CritRegion();
        critHelp.NotRelateLock();
        critHelp.CritRegion();
    }
    
    @Locking
    public void failedLock2(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.Release();
    }
    
    @Locking
    public void failedLock3(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.CritRegion();
    }
    
    @Unlocking
    public void failedLock4(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.Release();
        critHelp.EntAndLeaCrit();
        critHelp.Acquire();
        critHelp.Release();
        // :: error: (undefined.sequencing)
        critHelp.CritRegion();
    }
    
    @Locking
    public void failedLock5(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.EntAndLeaCrit();
        critHelp.NotRelateLock();
        // :: error: (undefined.sequencing)
        critHelp.Release();
    }
    
    @Unlocking
    public void somethingUnlock(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.CritRegion();
        critHelp.CritRegion();
        critHelp.NotRelateLock();
        critHelp.Release();
    }
    
    @Unlocking
    public void somethingUnlock1(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.Release();
        critHelp.NotRelateLock();
        critHelp.EntAndLeaCrit();
    }
    
    @Unlocking
    public void failedUnlock2(CoreCriticalTest.CriticalTestHelper critHelp) {
        critHelp.NotRelateLock();
        critHelp.CritRegion();
        critHelp.Release();
        critHelp.Acquire();
        // :: error: (undefined.sequencing)
        critHelp.Acquire();
    }
    
    @Unlocking
    public void failedUnlock3(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.Acquire();
    }
    
    @Unlocking
    public void failedUnlock4(CoreCriticalTest.CriticalTestHelper critHelp) {
        // :: error: (undefined.residual)
        critHelp.EntAndLeaCrit();
    }
    
}

