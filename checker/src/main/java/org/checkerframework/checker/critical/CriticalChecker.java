package org.checkerframework.checker.critical;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.genericeffects.EffectQuantale;
import org.checkerframework.checker.genericeffects.GenericEffectChecker;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})

public class CriticalChecker extends GenericEffectChecker<Class<? extends Annotation>> {
    
    @Override
    public EffectQuantale<Class<? extends Annotation>> getEffectLattice() {
        if (lattice == null) {
            lattice = new CriticalQuantale();
        }
        return lattice;
    }
    
    @Override
    public Class<? extends Annotation> fromAnnotation(Class<? extends Annotation> anno) {
        return anno;
    }
}
