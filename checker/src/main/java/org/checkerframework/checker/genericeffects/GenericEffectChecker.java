package org.checkerframework.checker.genericeffects;

import java.lang.annotation.Annotation;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * Base class providing reusable infrastructure for implementing effect systems in the Checker
 * Framework.
 */
@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})
public abstract class GenericEffectChecker<X> extends BaseTypeChecker {

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new GenericEffectVisitor<X>(this, getExtension(), this::fromAnnotation);
  }

  /** Reference to the implemented effect quantale */
  protected EffectQuantale<X> lattice;

  /**
   * Method to get the lattice of the checker.
   *
   * @return A EffectQuantale object that represents the lattice of the checker.
   */
  public abstract EffectQuantale<X> getEffectLattice();

  public GenericEffectExtension<X> getExtension() {
    return new GenericEffectExtension<X>(this.getEffectLattice());
  }

  public abstract X fromAnnotation(Class<? extends Annotation> anno);


  public boolean nontrivialSynchronized() { return false; }
  public X startSync() { return null; }
  public X endSync() { return null; }
}
