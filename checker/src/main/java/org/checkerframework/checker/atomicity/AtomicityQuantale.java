package org.checkerframework.checker.atomicity;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.atomicity.qual.Atomic;
import org.checkerframework.checker.atomicity.qual.Both;
import org.checkerframework.checker.atomicity.qual.Left;
import org.checkerframework.checker.atomicity.qual.NonAtomic;
import org.checkerframework.checker.atomicity.qual.Right;
import org.checkerframework.checker.genericeffects.EffectQuantale;
import org.checkerframework.javacutil.BugInCF;

public class AtomicityQuantale extends EffectQuantale<Class<? extends Annotation>> {

  public final Class<? extends Annotation> A = Atomic.class;
  public final Class<? extends Annotation> B = Both.class;
  public final Class<? extends Annotation> L = Left.class;
  public final Class<? extends Annotation> R = Right.class;
  public final Class<? extends Annotation> N = NonAtomic.class;
  public final ArrayList<Class<? extends Annotation>> effects = new ArrayList<>();

  public AtomicityQuantale() {
    effects.add(A);
    effects.add(B);
    effects.add(R);
    effects.add(L);
    effects.add(N);
  }

  @Override
  public boolean LE(Class<? extends Annotation> left, Class<? extends Annotation> right) {
    assert (left != null);
    assert (right != null);
    // We *could* implement this by returning (LUB(left,right)==right), but this will be
    // (marginally) faster
    return (right == N)
        || // N is top
        (left == B)
        || // B is bottom
        (left == right)
        || // reflexivity
        (left == R && right != L && right != B)
        || // only L and B are *not* LE R
        (left == L && right != R && right != B) // only R and B are *not* LE L
    ; // We've covered all cases: A is only <= A and <= N, handled by refl and top cases
  }

  @Override
  public Class<? extends Annotation> LUB(
      Class<? extends Annotation> left, Class<? extends Annotation> right) {
    assert (left != null);
    assert (right != null);
    if (left == B || left == right) {
      return right;
    } else if (right == B) {
      return left;
    } else if (left == N || right == N) {
      return N;
    } else {
      // At this point we know this is not an idempotent join, and neither is top or bottom. So both
      // are L, R, or A, and they are distinct.
      // Any join of two of those three will be A
      return A;
    }
  }

  @Override
  public Class<? extends Annotation> seq(
      Class<? extends Annotation> left, Class<? extends Annotation> right) {
    assert (left != null);
    assert (right != null);
    if (left == N || right == N) {
      return N;
    } else if (left == B) {
      return right;
    } else if (right == B) {
      return left;
    } else {
      // switch (left) {
      if (left == R) {
        // Sequencing after a right-mover, cases for N or B other already handled
        if (right == R) return R;
        if (right == A) return A;
        if (right == L) return A;
        assert false : "Unhandled seq(" + left + "," + right + ")"; 
        return null;
      } else if (left == L) {
        // Sequencing after a right-mover, cases for N or B other already handled
        if (right == R) return N;
        if (right == L) return L;
        if (right == A) return N;
        assert false : "Unhandled seq(" + left + "," + right + ")";
        return null;
      } else if (left == A) {
        // Sequencing after an atomic, N and B already handled
        if (right == R) return N;
        if (right == L) return A;
        if (right == A) return N;
        assert false : "Unhandled seq(" + left + "," + right + ")";
        return null;
      }
    }
    assert false;
    return null;
  }

  @Override
  public ArrayList<Class<? extends Annotation>> getValidEffects() {
    return effects;
  }

  @Override
  public Class<? extends Annotation> unit() {
    return B;
  }

  @Override
  public Class<? extends Annotation> iter(Class<? extends Annotation> x) {
    // Iteration is the identity, except for iterating atomic actions
    return (x == A ? N : x);
  }

  @Override
  public Class<? extends Annotation> residual(
      Class<? extends Annotation> sofar, Class<? extends Annotation> target) {
    // Anything can be sequenced after B and obtain itself, since B is unit
    if (sofar == B) {
      return target;
    }
    if (target == N) return N;
    if (target == A) {
      if (sofar == L) {
        // L\A=L
        return L;
      } else if (sofar == R) {
        // R\A=A
        return A;
      } else if (sofar == A) {
        // A\A=L
        return L;
      }
      // N\A is undef, B\A was handled above
      return null;
    }
    if (target == L) {
      // The only X such that X\L=L are B (handled above) and L
      return (sofar == L ? L : null);
    }
    if (target == R) {
      // The only X such that X\R=R are B (handled above) and R
      return (sofar == R ? R : null);
    }
    if (target == B) {
      return B;
    }
    throw new BugInCF("Unhandled residual " + sofar + " \\ " + target);
  }
}
