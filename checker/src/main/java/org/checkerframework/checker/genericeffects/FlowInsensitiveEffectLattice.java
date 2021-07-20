package org.checkerframework.checker.genericeffects;

public abstract class FlowInsensitiveEffectLattice<X> extends EffectQuantale<X> {

  @Override
  public final X unit() {
    return bottomEffect();
  }

  @Override
  public final X seq(X sofar, X target) {
    return LUB(sofar, target);
  }

  public abstract X bottomEffect();

  @Override
  public final X iter(X x) {
    return x;
  }

  @Override
  public final X residual(X sofar, X target) {
    // For joins, residual is just partial max function
    return (LE(sofar, target) ? target : null);
  }

  @Override
  public boolean isCommutative() { return true; }
}
