package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
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
 * @param <X> The representation type of the underlying effect quantale.
 */
public class ControlEffectQuantale<X>
    extends EffectQuantale<ControlEffectQuantale.ControlEffect<X>> {

  /** A representation type for a control effect. */
  public static class ControlEffect<X> {

    public final X base;
    public final Map<Class<?>, Set<LocatedEffect<X>>> excMap;
    public final Set<X> breakset;

    ControlEffect(X base, Map<Class<?>, Set<LocatedEffect<X>>> excMap, Set<X> breakset) {
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

  // Should this be EffectQuantale<X>? and param this by <X extends EffectQuantale<X>>

  private static class LocatedEffect<X> {
    public final X effect;
    public final Tree loc;

    public LocatedEffect(X e, Tree l) {
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

  private EffectQuantale<X> underlying;

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
    Set<X> bset;

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
      for (Class<?> exc : r.excMap.keySet()) {
        if (l.excMap.get(exc) == null) {
          m.put(exc, r.excMap.get(exc));
        }
      }
      emap = m;
    }

    return new ControlEffect<X>(base, emap, bset);
  }

  // TODO: This makes clear that I can't give reasonable error messages unless the visitor has
  // direct access
  // to why these control-lifted ops fail.  Having a ControlEffect<X> type is useful, and having the
  // Context track them as these triples will be useful, and this class should still define these
  // ops, but mostly for testing: the individual sub-cases here should be exposed publicly for the
  // visitor to access.
  @Override
  public ControlEffect<X> seq(ControlEffect<X> l, ControlEffect<X> r) {
    X base;
    Map<Class<?>, Set<LocatedEffect<X>>> emap;
    Set<X> bset;

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
        return null;
      }
    }

    Set<X> sndInCtxt = new HashSet<>();
    for (X x : r.breakset) {
      X tmp = underlying.seq(l.base, x);
      if (x == null) {
        return null;
      }
      sndInCtxt.add(tmp);
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
          return null;
        }
        s.add(new LocatedEffect<>(tmp, leff.loc));
      }
      if (lpartial == null) {
        emap.put(exc, s);
      } else {
        // already some stuff for this exception, update in place
        lpartial.addAll(s);
      }
    }
    return new ControlEffect<X>(base, emap, bset);
  }

  @Override
  public ArrayList<ControlEffect<X>> getValidEffects() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ControlEffect<X> unit() {
    return ControlEffect.unit(underlying);
  }

  @Override
  public ControlEffect<X> iter(ControlEffect<X> x) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ControlEffect<X> residual(ControlEffect<X> sofar, ControlEffect<X> target) {
    // TODO Auto-generated method stub
    return null;
  }
}
