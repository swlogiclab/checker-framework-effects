package org.checkerframework.checker.androidthreading;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.genericeffects.EffectQuantale;
import org.checkerframework.checker.genericeffects.GenericEffectChecker;
import org.checkerframework.checker.genericeffects.GenericEffectExtension;
import org.checkerframework.checker.genericeffects.GenericEffectVisitor;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * Checker for enforcing the Android threading annotations as a sound effect system.
 *
 * <p>See documentation on Thread annotations:
 * https://developer.android.com/studio/write/annotations.html#thread-annotations and on adding
 * these annotations to the class path for a project:
 * https://developer.android.com/studio/write/annotations.html#adding-library
 */
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
  public EffectQuantale<Class<? extends Annotation>> getEffectLattice() {
    if (lattice == null) {
      try {
        lattice = new AndroidThreadEffects();
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "Failed to load Android annotations from android.support.annotation; is the class file"
                + " in your class path?\n"
                + "It is typically located under a subdirectory of"
                + " extras/android/m2repository/com/android/support/support-annotations/ inside"
                + " your Android JDK.");
      }
    }
    return lattice;
  }
}
