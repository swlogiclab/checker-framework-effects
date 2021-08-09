package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * Base class providing reusable infrastructure for implementing effect systems in the Checker
 * Framework.
 */
@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})
public class CastingEffectChecker extends GenericEffectChecker<Class<? extends Annotation>> {

  @Override
  public EffectQuantale<Class<? extends Annotation>> getEffectLattice() {
    if (lattice == null) {
      lattice = new CastingEffects();
    }
    return lattice;
  }

  @Override
  public GenericEffectExtension<Class<? extends Annotation>> getExtension() {
    return new CastingEffectsExtension(this.getEffectLattice());
  }

  @Override
  public Class<? extends Annotation> fromAnnotation(Class<? extends Annotation> anno) {
    return anno;
  }
}
