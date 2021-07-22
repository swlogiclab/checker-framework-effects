package org.checkerframework.checker.androidthreading;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.FlowInsensitiveEffectLattice;
import org.checkerframework.checker.signature.qual.ClassGetName;

/**
 * Implementation of an effect system for soundly enforcing Android's threading annotations.
 *
 * <p>See documentation on Thread annotations:
 * https://developer.android.com/studio/write/annotations.html#thread-annotations and on adding
 * these annotations to the class path for a project:
 * https://developer.android.com/studio/write/annotations.html#adding-library
 */
public final class AndroidThreadEffects
    extends FlowInsensitiveEffectLattice<Class<? extends Annotation>> {

  /** Precomputed list of supported effects */
  private ArrayList<Class<? extends Annotation>> effects = new ArrayList<>();
  /** Shorthand reflected reference for android.support.annotation.MainThread */
  public final Class<? extends Annotation> MainThread;
  /** Shorthand reflected reference for android.support.annotation.UiThread */
  public final Class<? extends Annotation> UiThread;
  /** Shorthand reflected reference for android.support.annotation.WorkerThread */
  public final Class<? extends Annotation> WorkerThread;
  /** Shorthand reflected reference for android.support.annotation.BinderThread */
  public final Class<? extends Annotation> BinderThread;
  /** Shorthand reflected reference for android.support.annotation.AnyThread */
  public final Class<? extends Annotation> AnyThread;

  /**
   * Convenience routine to encapsulate the required cast when looking up Android ADK annotations
   *
   * @param s Fully-qualified class name of an annotation
   * @return The class object for the specified annotation
   * @throws ClassNotFoundException when the Android annotations cannot be found (usually a
   *     classpath issue)
   */
  @SuppressWarnings("unchecked")
  private Class<? extends Annotation> getAnnotation(@ClassGetName String s)
      throws ClassNotFoundException {
    return (Class<? extends Annotation>) Class.forName(s);
  }

  /**
   * Constructor to initialize the AndroidEffects instance
   *
   * @throws ClassNotFoundException when the Android annotations cannot be found (usually a
   *     classpath issue)
   */
  public AndroidThreadEffects() throws ClassNotFoundException {
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

    if (left == AnyThread) {
      return right;
    } else if (right == AnyThread) {
      return left;
    }
    return left == right ? left : null;
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
