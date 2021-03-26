package org.checkerframework.checker.upload;

//import org.checkerframework.checker.upload.qual.Noop;
//import org.checkerframework.checker.upload.qual.DoWorkOnDisk;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.checker.genericeffects.*;
//import org.checkerframework.framework.source.Result;
//import org.checherframework.checker.upload.Upload
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class UploadAnnotatedTypeFactory extends GenericEffectTypeFactory {	

	//private UploadEffectLattice UploadEffect;

	/**
     	* Constructor for the checker's type factory.
     	*
     	* @param checker The checker object that allows the type factory to access the lattice of the
     	*     checker.
     	* @param spew Boolean used for debugging.
     	*/

	public UploadAnnotatedTypeFactory(BaseTypeChecker checker, boolean spew){
		super(checker, false);
		//upload = ((UploadEffectChecker) checker).getEffectLatice();

		//debugSpew = spew;
		this.postInit();
	}
}
