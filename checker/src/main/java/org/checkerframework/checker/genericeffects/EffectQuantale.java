package org.checkerframework.checker.genericeffects;

import java.util.ArrayList;

/**
 * Core algebraic abstraction of flow-sensitive effects, modeling effects as a (partial)
 * join-semilattice-ordered (partial) monoid.
 *
 * <p>For full details, see Gordon's TOPLAS 2021 paper "Polymorphic Iterable Sequential Effect
 * Systems"
 *
 * @param <X> The representation type of behaviors handled by this effect quantale
 */
public abstract class EffectQuantale<X> {

  /**
   * Determine if one effect is less than or equal to another.
   *
   * <p>This method should <i>always</i> be functionally equivalent to returning <code>
   * LUB(left, right).equals(right)</code> since least-upper-bound defines an ordering. But for many
   * systems, it is possible to give a more efficient direct implementation.
   *
   * @param left Possible smaller/lesser effect
   * @param right Possible larger/greater effect
   * @return true if the first is less than or equal to the other / if the second is an upper bound
   *     of the first
   */
  public boolean LE(X left, X right) {
    return LUB(left, right).equals(right);
  }

  /**
   * Compute the least upper bound of two effects, if it exists.
   *
   * <p>This method is required to be commutative and associative, i.e.
   *
   * <ul>
   *   <li><code>LUB(x,y) == LUB(y,x)</code>
   *   <li><code>LUB(x,LUB(y,z)) == LUB(LUB(x,y),z)</code>
   * </ul>
   *
   * when all the mentioned least-upper-bounds are defined.
   *
   * @param l One effect
   * @param r Another effect
   * @return Their least upper bound if it exists, otherwise null
   */
  public abstract X LUB(X l, X r);

  /**
   * Compute the result of performing two effects in order, if that ordering is valid.
   *
   * <p>This method is required to be associative, i.e. <code>LUB(x,LUB(y,z)) == LUB(LUB(x,y),z)
   * </code> when all the mentioned least-upper-bounds are defined.
   *
   * @param l The first effect
   * @param r The second effect
   * @return The effect corresponding to performing these effects in order, if valid, otherwise
   *     null.
   */
  public abstract X seq(X l, X r);

  /**
   * Get the collection of supported effects.
   *
   * @return A list of effects supported by the checker
   */
  public abstract ArrayList<X> getValidEffects();

  /**
   * The unit element of the effect quantale, such that: <code>seq(x,unit()) == x == seq(unit(),x)
   * </code>
   *
   * @return The unit effect
   */
  public abstract X unit();

  /**
   * Compute the over-approximation of repeating an effect any finite number of times. If it exists.
   *
   * <p>This is required to satisfy 5 general properties:
   *
   * <ul>
   *   <li><code>LE(x,iter(x))</code>
   *   <li>if <code>LE(x,y)</code> then <code>LE(iter(x),iter(y)</code>
   *   <li><code>iter(iter(x))==iter(x)</code>
   *   <li><code>LE(unit(),iter(x))</code>
   *   <li><code>LE(seq(iter(x),iter(x)),iter(x))</code>
   * </ul>
   *
   * @param x Effect/behavior to repeat
   * @return An effect over-approximating any finite repetition of the argument, or null if no such
   *     effect is represented.
   */
  public abstract X iter(X x);

  /**
   * Returns the greatest effect which, when sequenced <i>after</i> the first effect, is less than
   * or equal to the second effect. This may not exist, in which case the operation returns null.
   *
   * @param sofar The effect executed thus far.
   * @param target The upper bound on sequencing the first argument and the result
   * @return The greatest effect y such that <code>LE(seq(sofar,y),target)</code>
   */
  public abstract X residual(X sofar, X target);

  /**
   * An accessor returning whether sequencing is also commutative. This is not required of effect
   * quantales, but when true a number of efficiencies and better error reporting are possible.
   *
   * <p>Implementors should never override this directly. Instead, they should subclass {@link
   * FlowInsensitiveEffectLattice} instead, which gives several simplifications and optimizations
   * for free.
   *
   * @return Whether or not this effect quantale's sequencing is commutative.
   */
  public boolean isCommutative() {
    return false;
  }
}
