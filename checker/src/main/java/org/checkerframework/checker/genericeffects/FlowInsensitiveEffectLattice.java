package org.checkerframework.checker.genericeffects;

/**
 * A specialization of {@link EffectQuantale} to the commutative case.
 *
 * <p>While effect quantales subsume any flow-insensitive / commutative effect system in theory, in
 * practice the framework can give better error reports if it knows a particular effect system is
 * commutative, which is what this base class is for. In addition to swapping the {@link
 * isCommutative()} result, it also gives default implementations of residuals, iteration, and
 * sequencing, so developers only need to provide least-upper-bound and a unit element.
 */
public abstract class FlowInsensitiveEffectLattice<X> extends EffectQuantale<X> {

  @Override
  public final X unit() {
    return bottomEffect();
  }

  @Override
  public X seq(X sofar, X target) {
    return LUB(sofar, target);
  }

  /**
   * For commutative effect systems, the least / smallest effect or behavior. This is in fact the
   * unit effect, but this name is often more natural.
   *
   * @return The least effect, which is also the unit for the effect system.
   */
  public abstract X bottomEffect();

  @Override
  public X iter(X x) {
    return x;
  }

  @Override
  public X residual(X sofar, X target) {
    // For joins, residual is just partial max function
    return (LE(sofar, target) ? target : null);
  }

  @Override
  public final boolean isCommutative() {
    return true;
  }
}
