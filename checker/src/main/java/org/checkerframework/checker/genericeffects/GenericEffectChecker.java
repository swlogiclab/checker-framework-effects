package org.checkerframework.checker.genericeffects;

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
