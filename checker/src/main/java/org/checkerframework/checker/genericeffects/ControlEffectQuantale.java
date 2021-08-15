package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type.ClassType;

import org.w3c.dom.events.EventTarget;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.BugInCF;

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
    public final Map<ClassType, Set<NonlocalEffect<X>>> excMap;
    /** Effects up to points where breaks cause non-local exits from cases and loops */
    public final Set<NonlocalEffect<X>> breakset;

    ControlEffect(
        X base, Map<ClassType, Set<NonlocalEffect<X>>> excMap, Set<NonlocalEffect<X>> breakset) {
      assert (excMap == null || excMap.size() > 0);
      assert (breakset == null || breakset.size() > 0);
      assert (base != null || excMap != null || breakset != null);
      this.base = base;
      this.excMap = excMap;
      this.breakset = breakset;
    }

    @Override
    public String toString() {
      return "["+base+"|"+excMap+"|"+breakset+"]";
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (null == o || !(o instanceof ControlEffect<?>)) return false;
      ControlEffect<X> other = (ControlEffect<X>)o;
      boolean baseOK = this.base == other.base || (this.base != null && this.base.equals(other.base));
      boolean excOK = this.excMap == other.excMap || (this.excMap != null && this.excMap.equals(other.excMap));
      boolean brkOK = this.breakset == other.breakset || (this.breakset != null && this.breakset.equals(other.breakset));
      return baseOK && excOK && brkOK;
    }

    @Override
    public int hashCode() {
      return base.hashCode() + excMap.hashCode() + breakset.hashCode();
    }

    public static <X> ControlEffect<X> unit(EffectQuantale<X> u) {
      return new ControlEffect<>(u.unit(), null, null);
    }
    public static <X> ControlEffect<X> basic(X x) {
      return new ControlEffect<>(x, null, null);
    }
    public static <X> ControlEffect<X> raise(X prefix, ClassType exc, Tree target, Tree src) {
      Map<ClassType,Set<NonlocalEffect<X>>> throwmap = new HashMap<>();
      Set<NonlocalEffect<X>> tset = new HashSet<>();
      tset.add(new NonlocalEffect<X>(prefix, target, src));
      throwmap.put(exc, tset);
      return new ControlEffect<>(null, throwmap, null);
    }
    public static <X> ControlEffect<X> breakout(X prefix, Tree target, Tree src) {
      Set<NonlocalEffect<X>> bset = new HashSet<>();
      bset.add(new NonlocalEffect<X>(prefix, target, src));
      return new ControlEffect<>(null, null, bset);
    }
  }

  // Some helper methods for functional set ops
  private Set<NonlocalEffect<X>> union(Set<NonlocalEffect<X>> a, Set<NonlocalEffect<X>> b) {
    /*
    TODO: Okay, so the difficulty here is resolving when some effect should dominate another due to targeting the same node the same way

    Hmm. Could actually create the effects with origins *and targets* and then just focus on targets here.... Do all NonlocalEffects exist because they have targets?
    */
    assert (a != null);
    assert (b != null);
    // TODO: perf headache for large effects
    Set<NonlocalEffect<X>> result = new HashSet<>(a);
    result.addAll(b);
    Set<NonlocalEffect<X>> toRemove = new HashSet<>();
    for (NonlocalEffect<X> x : result) {
      for (NonlocalEffect<X> y : result) {
        if (x != y && x.LE(underlying, y)) {
          // x is over-approximated by y
          toRemove.add(x);
        }
      }
    }
    result.removeAll(toRemove);
    return result;
  }

  private Set<NonlocalEffect<X>> unionPossiblyNull(Set<NonlocalEffect<X>> a, Set<NonlocalEffect<X>> b) {
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
  public static class NonlocalEffect<X> {
    /** The underlying effect */
    public final X effect;
    /** The target node of this non-local effect */
    public final Tree target;
    /** The tree that caused this non-local effect (for error-reporting, not semantics) */
    public final Tree src;

    /** Build a NonlocalEffect */
    public NonlocalEffect(X e, Tree target, Tree src) {
      if (null == e)
        throw new IllegalArgumentException("Cannot construct a NonlocalEffect with a null effect");
      if (null == src)
        throw new IllegalArgumentException("Cannot construct a NonlocalEffect with a null src tree");
      effect = e;
      this.target = target;
      this.src = src;
    }

    public NonlocalEffect<X> copyWithPrefix(X newPrefix) {
      return new NonlocalEffect<>(newPrefix, target, src);
    }

    @Override
    public int hashCode() {
      return effect.hashCode() + (target == null ? 3 : target.hashCode()) + src.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ControlEffectQuantale.NonlocalEffect)) {
        return false;
      }
      @SuppressWarnings("rawtypes")
      // Using rawtype here to avoid issue with unchecked cast, since the generic param can't be
      // checked
      ControlEffectQuantale.NonlocalEffect o = (ControlEffectQuantale.NonlocalEffect) other;
      // We do not compare source! This results in possibly losing track of multiple sources in unions, which may result in us only reporting one error location when multiple exist.
      return effect.equals(o.effect) && (target == o.target || (target != null && target.equals(o.target)));
    }

    public boolean LE(EffectQuantale<X> underlying, NonlocalEffect<X> other) {
      return (other.target == null || target == other.target || (target != null && target.equals(other.target))) && underlying.LE(effect, other.effect);
    }

    @Override
    public String toString() {
      return effect.toString()+":"+(target == null ? "<null>" : target.hashCode());
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
    Map<ClassType, Set<NonlocalEffect<X>>> emap;
    Set<NonlocalEffect<X>> bset;

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
      Map<ClassType, Set<NonlocalEffect<X>>> m = new HashMap<>();
      for (ClassType exc : l.excMap.keySet()) {
        // will be non-null by assumption
        Set<NonlocalEffect<X>> left = l.excMap.get(exc);
        // might be null
        Set<NonlocalEffect<X>> right = r.excMap.get(exc);
        m.put(exc, unionPossiblyNull(left, right));
      }
      // Then join in any exceptions thrown in the RHS but not in the left
      for (ClassType exc : r.excMap.keySet()) {
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
    Map<ClassType, Set<NonlocalEffect<X>>> emap;
    Set<NonlocalEffect<X>> bset;

    assert (lastErrors == null)
        : "ControlEffect.seq called without retrieving errors of prior call";

    if (l.base == null) {
      // No LHS base effect, no overall base (or anything else)
      return l;
    }

    base = null;
    if (r.base == null) {
      base = null;
    } else {
      base = underlying.seq(l.base, r.base);
      if (base == null) {
        addSequencingError(l.base, r.base, null);
      }
    }

    Set<NonlocalEffect<X>> sndInCtxt = new HashSet<>();
    if (r.breakset != null) {
      for (NonlocalEffect<X> x : r.breakset) {
        X tmp = underlying.seq(l.base, x.effect);
        if (tmp == null) {
          addSequencingError(l.base, x.effect, x.src);
        } else {
          sndInCtxt.add(x.copyWithPrefix(tmp));
        }
      }
    }
    if (sndInCtxt.size() == 0)
      sndInCtxt = null;
    bset = unionPossiblyNull(l.breakset, sndInCtxt);

    // sequence exception maps

    // Need to join where common
    emap = new HashMap<>();
    if (l.excMap != null) {
      for (ClassType exc : l.excMap.keySet()) {
        // will be non-null by assumption
        Set<NonlocalEffect<X>> left = l.excMap.get(exc);
        emap.put(exc, new HashSet<>(left));
      }
    }
    if (r.excMap != null) {
      for (ClassType exc : r.excMap.keySet()) {
        Set<NonlocalEffect<X>> lpartial = l.excMap == null ? null : l.excMap.get(exc);
        Set<NonlocalEffect<X>> s = new HashSet<>();
        for (NonlocalEffect<X> leff : r.excMap.get(exc)) {
          X tmp = underlying.seq(l.base, leff.effect);
          if (tmp == null) {
            addSequencingError(l.base, leff.effect, leff.src);
          } else {
            s.add(leff.copyWithPrefix(tmp));
          }
        }
        if (lpartial == null) {
          emap.put(exc, s);
        } else {
          // already some stuff for this exception, update in place
          lpartial.addAll(s);
        }
      }
    }
    if (emap.size() == 0)
      emap = null;
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
    return underlying.getValidEffects();
  }

  public X underlyingUnit() {
    return underlying.unit();
  }
  @Override
  public ControlEffect<X> unit() {
    return ControlEffect.unit(underlying);
  }
  public ControlEffect<X> breakout(Tree target, Tree src) {
    return ControlEffect.breakout(underlying.unit(), target, src);
  }
  public ControlEffect<X> raise(ClassType exc, Tree target, Tree src) {
    return ControlEffect.raise(underlying.unit(), exc, target, src);
  }
  public ControlEffect<X> lift(X x) {
    return new ControlEffect<X>(x, null, null);
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
    Map<ClassType, Set<NonlocalEffect<X>>> exc = x.excMap != null ? new HashMap<>() : null;
    Set<NonlocalEffect<X>> brks = x.breakset != null ? new HashSet<>() : null;
    if (exc != null) {
      for (Map.Entry<ClassType, Set<NonlocalEffect<X>>> kv : x.excMap.entrySet()) {
        Set<NonlocalEffect<X>> s = new HashSet<>();
        for (NonlocalEffect<X> ctrleff : kv.getValue()) {
          X tmp = underlying.seq(underlying_iter, ctrleff.effect);
          if (tmp == null) {
            addSequencingError(underlying_iter, ctrleff.effect, ctrleff.src);
          } else {
            s.add(ctrleff.copyWithPrefix(tmp));
          }
        }
        exc.put(kv.getKey(), s);
      }
    }

    if (brks != null) {
      for (NonlocalEffect<X> brkeff : x.breakset) {
        X tmp = underlying.seq(underlying_iter, brkeff.effect);
        if (tmp == null) {
          addSequencingError(underlying_iter, brkeff.effect, brkeff.src);
        } else {
          brks.add(brkeff.copyWithPrefix(tmp));
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
    // TODO: Fix for exception inheritance
    if (sofar.base == null) {
      // anything sequenced after only non-local behaviors has no impact on the overall effect, so really the residual could return anything and stuff would "work" so long as sofar <= target.
      // TODO: double-check the math to make sure I'm not missing a subtlety here
      if (LE(sofar, target)) {
        return target;
      } else {
        // sofar isn't less than target, and we can't sequence anything on the right of it to make it so
        return null;
      }
    }
    // Optimize common case
    if (sofar.breakset == null && sofar.excMap == null && target.base != null && target.breakset == null && target.excMap == null) {
      X baseResid = underlying.residual(sofar.base, target.base);
      if (baseResid == null) {
        return null;
      }
      return lift(baseResid);
    } else {
      // Base residual 
      X baseResid = sofar.base == null || target.base == null ? null : underlying.residual(sofar.base, target.base);
      if (baseResid == null && sofar.base != null && target.base != null) {
        // Underlying residual is undefined/invalid
        throw new BugInCF("WIP: undefined underlying residual");//return null;
      }

      // Control effects do not extend on the right, so any control effect in sofar must be over-approximated by a control effect in target.  Moreover, every control effect in target must have the sofar behavior as its "prefix", and so must have its residual with that underlying prefix defined.
      // Actually, refine that: if there is a control effect in target whose residual with the sofar.base is undefined, then that control effect *must* have been triggered in sofar, and cannot be triggered by further composition on the right. If we cannot find such an already-triggered control effect, then it's an error and there is no residual.

      Set<NonlocalEffect<X>> breakset = new HashSet<>();
      if (sofar.breakset != null && target.breakset == null) {
        // Target has no breaks, so no residual can be defined
        throw new BugInCF("WIP: target has no breaks but sofar does");//return null;
        // TODO: But wait, *as this is currently used* this will reject all breaks, since the residual is always checked w.r.t. method annotation.
        // TODO: This is still the correct definition of residual! But it suggests that the target should change inside valid domains of breaks (loops/switches). Suggests maybe some more theory work to do...
      }
      Set<X> overApprox = new HashSet<>();
      if (sofar.breakset != null) {
        for (NonlocalEffect<X> bsofar : sofar.breakset) {
          boolean matched = false;
          for (NonlocalEffect<X> btarget : target.breakset) {
            if (underlying.LE(bsofar.effect, btarget.effect)) {
              overApprox.add(btarget.effect);
              matched = true;
              break;
            }
          }
          if (!matched) {
            // This effect isn't over-approximated
            // TODO: extend lastErrors to handle these
            throw new BugInCF("WIP: break effect "+bsofar.effect+" in sofar has no over-approx in"+target.breakset);//return null;
          }
        }
      }
      // Okay, every existing break in sofar is matched to something in target
      // Now we need to check for every target behavior that it's either a suffix of sofar.base or over-approximates something in sofar (maybe we should build this mapping from target break to sofar break above to avoid more loops)
      if (target.breakset != null) {
        for (NonlocalEffect<X> btarget : target.breakset) {
          X underlyingResid = underlying.residual(sofar.base, btarget.effect);
          if (underlyingResid == null) {
            // Ok if this was over-approx of something so far, otherwise an error
            if (!overApprox.contains(btarget.effect)) {
              throw new BugInCF("WIP: target has break of "+btarget.effect+" but this has no residual with "+sofar.base+" and doesn't over-approximate anything in "+sofar.breakset);//return null;
            }
          } else {
            // This can still be raised later
            breakset.add(btarget.copyWithPrefix(underlyingResid));
          }
        }
      }
      if (breakset.size() == 0) breakset = null;

      // exceptions are the same as breaksets, but per-exception, and sofar shouldn't throw exceptions the target doesn't
      // TODO: Again, suggests the base for the residual check should be adjusted at try/catch/finally boundaries
      Map<ClassType,Set<NonlocalEffect<X>>> excMap = new HashMap<>();
      Map<ClassType,Set<X>> overApproxExc = new HashMap<>();
      if (sofar.excMap != null) {
        for (Map.Entry<ClassType,Set<NonlocalEffect<X>>> exc : sofar.excMap.entrySet()) {
          overApproxExc.put(exc.getKey(), new HashSet<>());
          if (!target.excMap.containsKey(exc.getKey())) {
            // Sofar throws something the target doesn't allow at all
            throw new BugInCF("WIP: sofar throws "+exc.getKey()+" but target doesn't");//return null;
          } else {
            // This exception type is thrown by both, handle similarly to breaksets
            Set<NonlocalEffect<X>> targetset = target.excMap.get(exc.getKey());
            for (NonlocalEffect<X> esofar : exc.getValue()) {
              boolean matched = false;
              for (NonlocalEffect<X> etarget : targetset) {
                if (underlying.LE(esofar.effect, etarget.effect)) {
                  overApproxExc.get(exc.getKey()).add(etarget.effect);
                  matched = true;
                  break;
                } 
              }
              if (!matched) {
                // not over-approximated for this exception
                //throw new BugInCF("WIP: sofar throws "+exc.getKey()+" after "+esofar.effect+" but this is not bounded by"+targetset);
                return null;
              }
            }
          }
        }
      }
      // Now we've concluded all exceptional behaviors in sofar are valid, and we now need to check all the exceptional behaviors in target are completeable after base
      if (target.excMap != null) {
        for (Map.Entry<ClassType,Set<NonlocalEffect<X>>> exc : target.excMap.entrySet()) {
          Set<NonlocalEffect<X>> residThrows = null;
          for (NonlocalEffect<X> etarget : exc.getValue()) {
            X underlyingResid = underlying.residual(sofar.base, etarget.effect);
            if (underlyingResid == null) {
              // ok only if over-approximating
              //Set<X> overApproxThisEffect = overApproxExc.get(exc.getKey());
              //if (overApproxThisEffect == null || !overApproxThisEffect.contains(etarget.effect)) {
              //  throw new BugInCF("WIP: target throws "+exc.getKey()+" after "+etarget.effect+" but this has no residual with sofar.base="+sofar.base+" and bounds nothing in "+sofar);//return null;
              //}
              // ACTUALLY okay regardless, because if we just drop this behavior, sequencing sofar with the result will still be less than target (i.e., the thinking above reflected aiming to be equal to it, which isn't the right goal)
            } else {
              // can still be thrown later
              if (residThrows == null) {
                residThrows = new HashSet<>();
              }
              residThrows.add(etarget.copyWithPrefix(underlyingResid));
            }
          }
          if (residThrows != null) excMap.put(exc.getKey(), residThrows);
        }
      }
      if (excMap.size() == 0) excMap = null;

      return new ControlEffect<X>(baseResid, excMap, breakset);
    }
  }

  @Override
  public boolean isCommutative() {
    return underlying.isCommutative();
  }
}
