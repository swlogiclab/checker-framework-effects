package org.checkerframework.checker.upload;

import org.checkerframework.checker.genericeffects.GenericEffectChecker;
import org.checkerframework.checker.genericeffects.GenericEffectExtension;
import org.checkerframework.checker.genericeffects.GenericEffectVisitor;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

@SupportedLintOptions({"debugSpew"})
@SupportedOptions({"ignoreEffects", "ignoreErrors", "ignoreWarnings"})
public class UploadChecker extends GenericEffectChecker {

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new GenericEffectVisitor(this, new GenericEffectExtension(getEffectLattice()));
  }
}
