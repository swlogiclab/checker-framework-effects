package org.checkerframework.checker.androidthreading;

import org.checkerframework.checker.genericeffects.GenericEffectChecker;
import org.checkerframework.checker.genericeffects.GenericEffectExtension;
import org.checkerframework.checker.genericeffects.GenericEffectLattice;
import org.checkerframework.checker.genericeffects.GenericEffectVisitor;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})
public class AndroidThreadingChecker extends GenericEffectChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new GenericEffectVisitor(this, new GenericEffectExtension(getEffectLattice()));
    }

    /**
     * Method to get the lattice of the checker.
     *
     * @return A GenericEffectLattice object that represents the lattice of the checker.
     */
    @Override
    public GenericEffectLattice getEffectLattice() {
        if (lattice == null) {
            try {
                lattice = AndroidThreadEffects.getAndroidThreadEffects();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                        "Failed to load Android annotations from android.support.annotation; is the class file in your class path?\nIt is typically located under a subdirectory of extras/android/m2repository/com/android/support/support-annotations/ inside your Android JDK.");
            }
        }
        return lattice;
    }
}
