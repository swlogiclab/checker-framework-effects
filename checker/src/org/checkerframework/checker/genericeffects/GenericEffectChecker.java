package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})
public class GenericEffectChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new GenericEffectVisitor(this, new CastingEffectsExtension(this.getEffectLattice()));
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

    /**
     * Method to get the lattice of the checker.
     *
     * @return A GenericEffectLattice object that represents the lattice of the checker.
     */
    public GenericEffectLattice getEffectLattice() {
        if (lattice == null) {
            lattice = new CastingEffects();
        }
        return lattice;
    }
}
