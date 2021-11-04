package org.checkerframework.checker.critical;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.critical.qual.Basic;
import org.checkerframework.checker.critical.qual.Critical;
import org.checkerframework.checker.critical.qual.Entrant;
import org.checkerframework.checker.critical.qual.Locking;
import org.checkerframework.checker.critical.qual.Unlocking;
import org.checkerframework.checker.genericeffects.EffectQuantale;
import org.checkerframework.javacutil.BugInCF;

public class CritQuantale extends EffectQuantale<Class<? extends Annotation>> {
    
    public final Class<? extends Annotation> B = Basic.class;
    public final Class<? extends Annotation> C = Critical.class;
    public final Class<? extends Annotation> E = Entrant.class;
    public final Class<? extends Annotation> L = Locking.class;
    public final Class<? extends Annotation> UL = Unlocking.class;
    public final ArrayList<Class<? extends Annotation>> effects = new ArrayList<>();

    
    public CritQuantale() {
        effects.add(B);
        effects.add(C);
        effects.add(E);
        effects.add(L);
        effects.add(UL);
    }
   
    @Override
    public Class<? extends Annotation> LUB (Class<? extends Annotation> EffectA, Class<? extends Annotation> EffectB) {
        if (EffectA == EffectB) {
            return EffectA;
        }
        if (EffectA == B) {
            if (EffectB == C || EffectB == E) {
                return EffectB;
            }
        }
        if (EffectB == B) {
            if (EffectA == C || EffectA == E) {
                return EffectA;
            }
        }
        return null;
    }
    
    @Override
    public Class<? extends Annotation> seq(Class<? extends Annotation> EffectA, Class<? extends Annotation> EffectB) {
        if (EffectA == B) {
            return EffectB;
        }
        else if (EffectB == B) {
            return EffectA;
        }
        else {
            if (EffectA == L) {
                if (EffectB == UL) return E;
                if (EffectB == C) return L;
                assert (false) : "Unhandled seq(" + EffectA + "," + EffectB + ")";
                return null;
            }
            else if (EffectA == UL) {
                if (EffectB == L) return C;
                if (EffectB == E) return UL;
                assert (false) : "Unhandled seq(" + EffectA + "," + EffectB + ")";
                return null;
            }
            else if (EffectA == C) {
                if (EffectB == UL) return UL;
                if (EffectB == C) return C;
                assert (false) : "Unhandled seq(" + EffectA + "," + EffectB + ")";
                return null;
            }
            else if (EffectA == E) {
                if (EffectB == L) return L;
                if (EffectB == E) return E;
                assert (false) : "Unhandled seq(" + EffectA + "," + EffectB + ")";
                return null;
            }
        }
        assert (false);
        return null;
    }
    
    @Override
    public ArrayList<Class<? extends Annotation>> getValidEffects() {
        return effects;
    }
    
    @Override
    public Class<? extends Annotation> unit() {
        return B;
    }
    
    @Override
    public Class<? extends Annotation> iter(Class<? extends Annotation> x) {
        if ( x == L || x == UL ) {
            return null; 
        }
        else {
            return x;
        }
    }
    
    @Override
    public Class<? extends Annotation> residual(Class<? extends Annotation> sofar, Class<? extends Annotation> target) {
        if (sofar == B) {
            return target;
        }
        if (target == E) {
            if (sofar == E) {
                return E;
            }
            else if (sofar == L) {
                return UL;
            }
            return null;
        }
        if (target == C) {
            if (sofar == C) {
                return C;
            }
            else if (sofar == UL) {
                return L;
            }
            return null;
        }
        if (target == B) {
            return B;
        }
        return null;
    }        
}


