package org.checkerframework.checker.androidthreading;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.FlowInsensitiveEffectLattice;

/*
 * See documentation on Thread annotations:
 *  https://developer.android.com/studio/write/annotations.html#thread-annotations
 * and on adding these annotations to the class path for a project:
 *  https://developer.android.com/studio/write/annotations.html#adding-library
 */
public final class AndroidThreadEffects
    extends FlowInsensitiveEffectLattice<Class<? extends Annotation>> {

  private ArrayList<Class<? extends Annotation>> effects = new ArrayList<>();
  public final Class<? extends Annotation> MainThread;
  public final Class<? extends Annotation> UiThread;
  public final Class<? extends Annotation> WorkerThread;
  public final Class<? extends Annotation> BinderThread;
  public final Class<? extends Annotation> AnyThread;

  public static AndroidThreadEffects getAndroidThreadEffects() throws ClassNotFoundException {
    return new AndroidThreadEffects();
  }

  @SuppressWarnings("unchecked")
  private Class<? extends Annotation> getAnnotation(String s) throws ClassNotFoundException {
    return (Class<? extends Annotation>) Class.forName(s);
  }

  private AndroidThreadEffects() throws ClassNotFoundException {
    // To avoid having the Checker Framework depend on the Android
    // SDK, we retrieve these qualifiers by reflection.
    MainThread = getAnnotation("android.support.annotation.MainThread");
    UiThread = getAnnotation("android.support.annotation.UiThread");
    WorkerThread = getAnnotation("android.support.annotation.WorkerThread");
    BinderThread = getAnnotation("android.support.annotation.BinderThread");
    AnyThread = getAnnotation("android.support.annotation.AnyThread");
    effects.add(MainThread);
    effects.add(UiThread);
    effects.add(WorkerThread);
    effects.add(BinderThread);
    effects.add(AnyThread);
  }

  @Override
  public Class<? extends Annotation> LUB(
      Class<? extends Annotation> left, Class<? extends Annotation> right) {
    assert (left != null && right != null);

    if (left.equals(AnyThread)) {
      return right;
    } else if (right.equals(AnyThread)) {
      return left;
    }
    return left.equals(right) ? left : null;
  }

  /** Get the collection of valid effects. */
  @Override
  public ArrayList<Class<? extends Annotation>> getValidEffects() {
    return effects;
  }

  /**
   * Get the Bottom Most Effect of Lattice. For IO EFfect checker: Bottom Most Effect of Lattice:
   * NoIOEffect
   */
  @Override
  public Class<? extends Annotation> bottomEffect() {
    return AnyThread;
  }
}
