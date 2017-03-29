package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;

@SupportedLintOptions({"debugSpew"})
public class GenericEffectChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new GenericEffectVisitor(this);
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Set<Class<? extends Annotation>> annos =
                ((BaseTypeVisitor<?>) visitor).getTypeFactory().getSupportedTypeQualifiers();
        if (annos.isEmpty()) {
            return super.getSuppressWarningsKeys();
        }

        Set<String> swKeys = new HashSet<>();
        swKeys.add(SUPPRESS_ALL_KEY);
        for (Class<? extends Annotation> anno : annos) {
            swKeys.add(anno.getSimpleName().toLowerCase());
        }

        return swKeys;
    }

    protected GenericEffectLattice lattice;

    public GenericEffectLattice getEffectLattice() {
        if (lattice != null) {
            lattice = new DefaultEffects();
        }
        return lattice;
    }
}
