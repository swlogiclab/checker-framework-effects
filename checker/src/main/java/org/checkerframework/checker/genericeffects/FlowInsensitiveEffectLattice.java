package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

public abstract class FlowInsensitiveEffectLattice implements GenericEffectLattice {

    @Override
    public boolean LE(Class<? extends Annotation> left, Class<? extends Annotation> right) {
        // TODO Auto-generated method stub
        return LUB(left,right)==right;
    }

    @Override
    public Class<? extends Annotation> unit() {
        return bottomEffect();
    }

    public abstract Class<? extends Annotation> bottomEffect();

    @Override
    public Class<? extends Annotation> iter(Class<? extends Annotation> x) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<? extends Annotation> residual(Class<? extends Annotation> sofar, Class<? extends Annotation> target) {
        // For joins, residual is just partial max function
        return (LE(sofar, target) ? target : null);
    }
    
}
