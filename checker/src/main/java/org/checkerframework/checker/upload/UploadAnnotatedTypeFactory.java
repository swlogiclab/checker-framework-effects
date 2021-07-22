package org.checkerframework.checker.upload;

// import org.checkerframework.checker.upload.qual.Noop;
// import org.checkerframework.checker.upload.qual.DoWorkOnDisk;
import org.checkerframework.checker.genericeffects.GenericEffectTypeFactory;
// import org.checkerframework.framework.source.Result;
// import org.checherframework.checker.upload.Upload
import org.checkerframework.common.basetype.BaseTypeChecker;

public class UploadAnnotatedTypeFactory extends GenericEffectTypeFactory {

  // private UploadEffectLattice UploadEffect;

  /**
   * Constructor for the checker's type factory.
   *
   * @param checker The checker object that allows the type factory to access the lattice of the
   *     checker.
   * @param spew Boolean used for debugging.
   */
  public UploadAnnotatedTypeFactory(BaseTypeChecker checker, boolean spew) {
    super(checker, false);
    // upload = ((UploadEffectChecker) checker).getEffectLatice();

    // debugSpew = spew;
    this.postInit();
  }
}
