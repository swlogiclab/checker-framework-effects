package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of Gordon's control effect transformation for effect quantales. For full
 * details, see Gordon's ECOOP 2020 paper "Lifting Sequential Effects to Control Operators." This
 * class implements the specialization of that to just tracking different classes of "aborting"
 * computation: throws, breaks, and continue statements. Some tracking is annotated with AST nodes
 * for better error reporting.
 *
 * <p>This implementation deviates in a couple modest ways from the system in the paper, largely
 * with respect to labels. The paper uses prompts explicitly tagged with labels, neither of which
 * exist in Java. Instead of labels, we tag non-local effects with their AST node of origin. Then
 * since there are no first-class continuations floating around in current Java, the rules for AST
 * nodes that correspond to prompt locations simply filter the appropriate sets of control effects
 * to those originating at subtrees of the "prompt location." Currently this includes:
 *
 * <ul>
 *   <li>Try blocks, for exceptions
 *   <li>Any loop (for, while, do-while) for breaks
 *   <li>Switch statements for breaks
 * </ul>
 *
 * @param <X> The representation type of the underlying effect quantale.
 */
public class ControlEffectQuantale<X>
    extends EffectQuantale<ControlEffectQuantale.ControlEffect<X>> {

  /** Reference to the underlying effect quantale's operations */
  private EffectQuantale<X> underlying;

  /** A representation type for a control effect. */
  public static class ControlEffect<X> {

    /** Base effect, null for absent */
    public final X base;
    /**
     * Effects up to points where (checked) exceptions are thrown.
     *
     * <p>Empty sets are represented by an empty collection OR null for efficiency of
     * <i>construction</i>. Most control effect instances will not have any exceptions or breaks.
     */
    public final Map<Class<?>, Set<LocatedEffect<X>>> excMap;
    /** Effects up to points where breaks cause non-local exits from cases and loops */
    public final Set<LocatedEffect<X>> breakset;

    ControlEffect(
        X base, Map<Class<?>, Set<LocatedEffect<X>>> excMap, Set<LocatedEffect<X>> breakset) {
      this.base = base;
      this.excMap = excMap;
      this.breakset = breakset;
    }

    public static <X> ControlEffect<X> unit(EffectQuantale<X> u) {
      return new ControlEffect<>(u.unit(), null, null);
    }
  }

  // Some helper methods for functional set ops
  private <T> Set<T> union(Set<T> a, Set<T> b) {
    Set<T> uset = new HashSet<>(a);
    uset.addAll(b);
    return uset;
  }

  private <T> Set<T> unionPossiblyNull(Set<T> a, Set<T> b) {
    if (null == a) {
      return b;
    } else if (null == b) {
      return a;
    } else {
      return union(a, b);
    }
  }

  /**
   * Representation of a non-local effect (i.e., a control effect in the paper) using the tree node
   * of origin (source of the throw or break) in lieu of a label, since these can be checked for
   * subtree relationships with prompt boundaries.
   */
  private static class LocatedEffect<X> {
    public final X effect;
    public final Tree loc;

    /** Build a LocatedEffect */
    public LocatedEffect(X e, Tree l) {
      if (null == e)
        throw new IllegalArgumentException("Cannot construct a LocatedEffect with a null effect");
      if (null == l)
        throw new IllegalArgumentException("Cannot construct a LocatedEffect with a null tree");
      effect = e;
      loc = l;
    }

    @Override
    public int hashCode() {
      return effect.hashCode() + loc.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ControlEffectQuantale.LocatedEffect)) {
        return false;
      }
      @SuppressWarnings("rawtypes")
      // Using rawtype here to avoid issue with unchecked cast, since the generic param can't be
      // checked
      ControlEffectQuantale.LocatedEffect o = (ControlEffectQuantale.LocatedEffect) other;
      return effect.equals(o.effect) && loc.equals(o.loc);
    }
  }

  public ControlEffectQuantale(EffectQuantale<X> u) {
    if (u == null) {
      throw new IllegalArgumentException(
          "Cannot construct control effect quantale from null underlying effect quantale");
    }
    underlying = u;
  }

  @Override
  public ControlEffect<X> LUB(ControlEffect<X> l, ControlEffect<X> r) {
    X base;
    Map<Class<?>, Set<LocatedEffect<X>>> emap;
    Set<LocatedEffect<X>> bset;

    // Join underlying effects
    if (l.base == null) {
      base = r.base;
    } else if (r.base == null) {
      base = l.base;
    } else {
      base = underlying.LUB(l.base, r.base);
      if (base == null) {
        return null;
      }
    }

    // Join break sets
    bset = unionPossiblyNull(l.breakset, r.breakset);

    // Join exception maps
    if (l.excMap == null) {
      emap = r.excMap;
    } else if (r.excMap == null) {
      emap = l.excMap;
    } else {
      // Need to join where common
      Map<Class<?>, Set<LocatedEffect<X>>> m = new HashMap<>();
      for (Class<?> exc : l.excMap.keySet()) {
        // will be non-null by assumption
        Set<LocatedEffect<X>> left = l.excMap.get(exc);
        // might be null
        Set<LocatedEffect<X>> right = r.excMap.get(exc);
        m.put(exc, unionPossiblyNull(left, right));
      }
      // Then join in any exceptions thrown in the RHS but not in the left
      for (Class<?> exc : r.excMap.keySet()) {
        if (l.excMap.get(exc) == null) {
          m.put(exc, r.excMap.get(exc));
        }
      }
      emap = m;
    }

    return new ControlEffect<X>(base, emap, bset);
  }

  public static class BadSequencing<X> {
    public final X left;
    public final X right;
    public final Tree rhs_source;

    public BadSequencing(X l, X r, Tree s) {
      if (null == l)
        throw new IllegalArgumentException("Cannot construct BadSequencing with null LHS");
      if (null == r)
        throw new IllegalArgumentException("Cannot construct BadSequencing with null RHS");
      this.left = l;
      this.right = r;
      this.rhs_source = s;
    }
  }

  private Set<BadSequencing<X>> lastErrors;

  public Collection<BadSequencing<X>> lastSequencingErrors() {
    Set<BadSequencing<X>> errs = lastErrors;
    lastErrors = null;
    return errs;
  }

  private void addSequencingError(X l, X r, Tree n) {
    if (lastErrors == null) {
      lastErrors = new HashSet<>();
    }
    lastErrors.add(new BadSequencing<X>(l, r, n));
  }

  // TODO: This makes clear that I can't give reasonable error messages unless the visitor has
  // direct access
  // to why these control-lifted ops fail.  Having a ControlEffect<X> type is useful, and having the
  // Context track them as these triples will be useful, and this class should still define these
  // ops, but mostly for testing: the individual sub-cases here should be exposed publicly for the
  // visitor to access.
  //
  // Idea: in general, there may be multiple reasons this operation could produce errors (compared
  // to lub, which only fails immediately if the base lub is undefined). So we could build an
  // ERRNO-like soluation, where if this fails, we set a member to contain a list of "reasons",
  // which will typically be a pair of two underlying effects that can't sequence, and an optional
  // tree node for the RHS (the throw/call/break for a control effect).  Expose this through a
  // method that retrieves the set and marks it null, then make this code make calls to an
  // "addErrorCause" method whenever something fails, which internally checks for null and allocates
  // the set if necessary. This will be minimal set allocation.
  @Override
  public ControlEffect<X> seq(ControlEffect<X> l, ControlEffect<X> r) {
    X base;
    Map<Class<?>, Set<LocatedEffect<X>>> emap;
    Set<LocatedEffect<X>> bset;

    assert (lastErrors == null)
        : "ControlEffect.seq called without retrieving errors of prior call";

    if (l.base == null) {
      // No LHS base effect, no overall base (or anything else)
      return l;
    }

    base = null;
    if (r.base == null) {
      base = l.base;
    } else {
      base = underlying.seq(l.base, r.base);
      if (base == null) {
        addSequencingError(l.base, r.base, null);
      }
    }

    Set<LocatedEffect<X>> sndInCtxt = new HashSet<>();
    for (LocatedEffect<X> x : r.breakset) {
      X tmp = underlying.seq(l.base, x.effect);
      if (tmp == null) {
        addSequencingError(l.base, x.effect, x.loc);
      } else {
        sndInCtxt.add(new LocatedEffect<>(tmp, x.loc));
      }
    }
    bset = unionPossiblyNull(l.breakset, sndInCtxt);

    // sequence exception maps

    // Need to join where common
    emap = new HashMap<>();
    for (Class<?> exc : l.excMap.keySet()) {
      // will be non-null by assumption
      Set<LocatedEffect<X>> left = l.excMap.get(exc);
      emap.put(exc, new HashSet<>(left));
    }
    for (Class<?> exc : r.excMap.keySet()) {
      Set<LocatedEffect<X>> lpartial = l.excMap.get(exc);
      Set<LocatedEffect<X>> s = new HashSet<>();
      for (LocatedEffect<X> leff : r.excMap.get(exc)) {
        X tmp = underlying.seq(l.base, leff.effect);
        if (tmp == null) {
          addSequencingError(l.base, leff.effect, leff.loc);
        } else {
          s.add(new LocatedEffect<>(tmp, leff.loc));
        }
      }
      if (lpartial == null) {
        emap.put(exc, s);
      } else {
        // already some stuff for this exception, update in place
        lpartial.addAll(s);
      }
    }
    // Error-free if lastErrors is still null
    if (lastErrors == null) {
      return new ControlEffect<X>(base, emap, bset);
    } else {
      return null;
    }
  }

  @Override
  public ArrayList<Class<? extends Annotation>> getValidEffects() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ControlEffect<X> unit() {
    return ControlEffect.unit(underlying);
  }

  @Override
  public ControlEffect<X> iter(ControlEffect<X> x) {
    assert (lastErrors == null)
        : "ControlEffect.iter called without retrieving errors of prior call";
    if (x.base == null) return null;
    X underlying_iter = underlying.iter(x.base);
    if (underlying_iter == null) {
      return null;
    }
    // Valid underlying iteration becomes result base, which must also be prefixed on every
    // exception or break control effect.
    // Technically this merges slightly early compared to the paper, but this is defined iff the
    // paper's version is defined, and gives more eager error messages
    Map<Class<?>, Set<LocatedEffect<X>>> exc = x.excMap != null ? new HashMap<>() : null;
    Set<LocatedEffect<X>> brks = x.breakset != null ? new HashSet<>() : null;
    if (exc != null) {
      for (Map.Entry<Class<?>, Set<LocatedEffect<X>>> kv : x.excMap.entrySet()) {
        Set<LocatedEffect<X>> s = new HashSet<>();
        for (LocatedEffect<X> ctrleff : kv.getValue()) {
          X tmp = underlying.seq(underlying_iter, ctrleff.effect);
          if (tmp == null) {
            addSequencingError(underlying_iter, ctrleff.effect, ctrleff.loc);
          } else {
            s.add(new LocatedEffect<>(tmp, ctrleff.loc));
          }
        }
        exc.put(kv.getKey(), s);
      }
    }

    if (brks != null) {
      for (LocatedEffect<X> brkeff : x.breakset) {
        X tmp = underlying.seq(underlying_iter, brkeff.effect);
        if (tmp == null) {
          addSequencingError(underlying_iter, brkeff.effect, brkeff.loc);
        } else {
          brks.add(new LocatedEffect<>(tmp, brkeff.loc));
        }
      }
    }

    if (lastErrors == null) {
      return new ControlEffect<X>(underlying_iter, exc, brks);
    } else {
      return null;
    }
  }

  /**
   * Computes the residual of two control effects.
   *
   * <p>In the notation of the original control effect paper, we're computing
   *
   * <p>{@code (X1,C1) \ (X2,C2) }
   *
   * <p>which is the largest <code>(X3,C3)</code> such that
   *
   * <p>{@code (X1,C1) |> (X2,C3) <= (X2,C2) }
   *
   * <p>This means we must take the residual of the underlying component, and then handle the break
   * and exception sets. By the equation above, we need:
   *
   * <p>{@code C1 U (X1 |> C3) <= C2 }
   *
   * <p>The largest such C3 would be <code>X1 \ (C2 - C1)</code>: C2 less those already complete
   * from C1, with residual by X1 lifted to sets of control behaviors.
   *
   * <p>Plus, some wrinkles for error reporting: generally this will be called with the target
   * effect as the method effect, which will have placeholder trees.
   *
   * <p>TODO: Oh! but actually that's not quite right and/or the use of this around prompt
   * boundaries must be refined. Control effects that will be resolved within the scope of the
   * target shouldn't figure into this result... or should, but should be handled appropriately...
   * which is subtle, because it includes cases like taking the residual within a try-catch, when
   * the body may throw locally-caught exceptions. So maybe:
   *
   * <ul>
   *   <li>Residual on underlying effects
   *   <li>For exceptions, residual of control w.r.t. underlying for exceptions in the target
   *   <li>For exceptions, not in the target, just pass through "hoping" they're caught locally,
   *       which will be checked by the actual try-catch rule
   *   <li>optimistically pass breaks through, since they might later feed into throw or completion
   *       or both, so we can't reject early?
   * </ul>
   *
   * This is along the right lines, but really we need a proof.
   */
  @Override
  public ControlEffect<X> residual(ControlEffect<X> sofar, ControlEffect<X> target) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isCommutative() {
    return underlying.isCommutative();
  }
}
