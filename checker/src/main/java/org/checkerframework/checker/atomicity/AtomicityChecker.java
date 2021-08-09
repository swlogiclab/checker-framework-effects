package org.checkerframework.checker.atomicity;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.genericeffects.EffectQuantale;
import org.checkerframework.checker.genericeffects.GenericEffectChecker;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})
public class AtomicityChecker extends GenericEffectChecker<Class<? extends Annotation>> {

  /**
   * Method to get the lattice of the checker.
   *
   * @return A GenericEffectLattice object that represents the lattice of the checker.
   */
  @Override
  public EffectQuantale<Class<? extends Annotation>> getEffectLattice() {
    if (lattice == null) {
      lattice = new AtomicityQuantale();
    }
    return lattice;
  }

  @Override
  public Class<? extends Annotation> fromAnnotation(Class<? extends Annotation> anno) {
    return anno;
  }
}
