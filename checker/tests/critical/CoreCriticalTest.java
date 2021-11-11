import org.checkerframework.checker.critical.qual.*;


public class CoreCriticalTest {
    
    public static interface CriticalTestHelper {
        
        @Locking
        public void Acquire();
        
        @Unlocking
        public void Release();
        
        @Entrant
        public void EntAndLeaCrit();
        
        @Critical
        public void CritRegion();
        
        @Basic
        public void NotRelateLock();
        
        @Locking
        public boolean AcquireBool();
        
        @Unlocking
        public boolean ReleaseBool();
        
        @Entrant
        public boolean EntAndLeaCritBool();
        
        @Critical
        public boolean CritRegionBool();
        
        @Basic
        public boolean NotRelateLockBool();
        
    }
    
    public static class BasicSubEffectTests {
        public CriticalTestHelper critHelp;
        
        @Locking
        public void lockSubLock() {
            critHelp.Acquire();
        }
        
        @Unlocking
        public void unlockSubUnlock() {
            critHelp.Release();
        }
        
        @Entrant
        public void basicSubEntrant() {
            critHelp.NotRelateLock();
        }
        
        @Critical
        public void basicSubCritical() {
            critHelp.NotRelateLock();
        }
    }
    
    
}