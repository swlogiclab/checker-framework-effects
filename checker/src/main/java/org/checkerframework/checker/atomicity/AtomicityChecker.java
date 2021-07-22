package org.checkerframework.checker.atomicity;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.genericeffects.EffectQuantale;
import org.checkerframework.checker.genericeffects.GenericEffectChecker;
import org.checkerframework.checker.genericeffects.GenericEffectExtension;
import org.checkerframework.checker.genericeffects.GenericEffectVisitor;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})
public class AtomicityChecker extends GenericEffectChecker {

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
      lattice = new AtomicityQuantale();
    }
    return lattice;
  }
}
