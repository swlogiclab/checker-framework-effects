package org.checkerframework.checker.upload;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.EffectQuantale;
import org.checkerframework.checker.upload.qual.Flush;
import org.checkerframework.checker.upload.qual.Noop;
import org.checkerframework.checker.upload.qual.Send;
import org.checkerframework.checker.upload.qual.WriteOnDisk;

public final class UploadEffects extends EffectQuantale<Class<? extends Annotation>> {

  private ArrayList<Class<? extends Annotation>> effects = new ArrayList<>();
  // public final Class<? extends Annotation> WriteOnDisk;
  // public final Class<? extends Annotation> Noop;

  public UploadEffects() {
    // WriteOnDisk = getAnnotation("upload.support.annotation.WriteOnDisk");
    effects.add(WriteOnDisk.class);
    effects.add(Noop.class);
    effects.add(Send.class);
    effects.add(Flush.class);
  }

  /**
   * Method to check Less than equal to Effect
   *
   * @param left : Left Effect
   * @param right : Right Effect
   * @return boolean true : if bottom effect is on the left and the top effect is on the right, or
   *     if effects are equal
   *     <p>false : otherwise
   */
  @Override
  public boolean LE(Class<? extends Annotation> left, Class<? extends Annotation> right) {
    assert (left != null && right != null);

    if (right == WriteOnDisk.class) return left == WriteOnDisk.class || left == Noop.class;
    else if (right == Noop.class) return left == Noop.class;

    return false;
  }

  @Override
  public Class<? extends Annotation> LUB(
      Class<? extends Annotation> left, Class<? extends Annotation> right) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<? extends Annotation> seq(
      Class<? extends Annotation> left, Class<? extends Annotation> right) {
    // TODO
    throw new UnsupportedOperationException();
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
  public Class<? extends Annotation> unit() {
    return Noop.class;
  }

  @Override
  public Class<? extends Annotation> residual(
      Class<? extends Annotation> sofar, Class<? extends Annotation> target) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<? extends Annotation> iter(Class<? extends Annotation> x) {
    // TODO
    throw new UnsupportedOperationException();
  }
}
