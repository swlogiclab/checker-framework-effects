package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import org.checkerframework.checker.genericeffects.qual.DecimalOverflow;
import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.SafeCast;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.TreeUtils;

public class GenericEffectTypeFactory extends BaseAnnotatedTypeFactory {

    protected final boolean debugSpew;
    private GenericEffectLattice genericEffect;

    public GenericEffectTypeFactory(BaseTypeChecker checker, boolean spew) {
        // use true to enable flow inference, false to disable it
        super(checker, false);

        genericEffect = ((GenericEffectChecker) checker).getEffectLattice();

        debugSpew = spew;
        this.postInit();
    }

    /**
     * Method to check if override method's effect is valid override
     *
     * @param overrider : Method in the subclass which is overriding the method of superclass
     * @param parentType : Parent type, whose method is being overridden
     * @return Overridden method : as Executable element null : if matching overridden method not
     *     found in Parent type
     */
    public ExecutableElement findJavaOverride(ExecutableElement overrider, TypeMirror parentType) {
        if (parentType.getKind() != TypeKind.NONE) {
            if (debugSpew) {
                System.err.println("Searching for overridden methods from " + parentType);
            }

            TypeElement overriderClass = (TypeElement) overrider.getEnclosingElement();
            TypeElement elem = (TypeElement) ((DeclaredType) parentType).asElement();
            if (debugSpew) {
                System.err.println("necessary TypeElements acquired: " + elem);
            }

            for (Element e : elem.getEnclosedElements()) {
                if (debugSpew) {
                    System.err.println("Considering element " + e);
                }
                if (e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement ex = (ExecutableElement) e;
                    boolean overrides = elements.overrides(overrider, ex, overriderClass);
                    if (overrides) {
                        return ex;
                    }
                }
            }
            if (debugSpew) {
                System.err.println("Done considering elements of " + parentType);
            }
        }
        return null;
    }

    //change this to be similar to ExecutableElement
    public Class<? extends Annotation> getDeclaredEffect(Element methodElt)
    {
        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect = null;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = getDeclAnnotation(methodElt, OkEffect);
            if (annotatedEffect != null) {
                if (debugSpew) {
                    System.err.println("Method marked @" + annotatedEffect);
                }
                return OkEffect;
            }
        }
        //this was originally bottom lattice
        //if a static initializer goes through here then that means that the enclosing element is a static initializer not a class
        AnnotationMirror anno = getDeclAnnotation(methodElt.getEnclosingElement(), DefaultEffect.class);
        if(anno == null || anno.getElementValues().entrySet().toArray().length == 0)
            return genericEffect.getBottomMostEffectInLattice();
        //this might not parse string safely
        String effect = anno.getElementValues().entrySet().toArray()[0].toString().split("=")[1].replace("\"", "");

        Class<? extends Annotation> defaultEffect = genericEffect.checkClassType(effect);
        return defaultEffect;
        //return genericEffect.getBottomMostEffectInLattice();
    }






    public Class<? extends Annotation> getDeclaredEffect(Element methodElt, Element clsElt)
    {
        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect = null;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = getDeclAnnotation(methodElt, OkEffect);
            if (annotatedEffect != null) {
                if (debugSpew) {
                    System.err.println("Method marked @" + annotatedEffect);
                }
                return OkEffect;
            }
        }
        //this was originally bottom lattice
        //if a static initializer goes through here then that means that the enclosing element is a static initializer not a class
        //AnnotationMirror anno = getDeclAnnotation(clsElt, DefaultEffect.class);
        //https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation

        String teStr = getClassType(clsElt);

        //this might not parse string safely
        //String effect = anno.getElementValues().entrySet().toArray()[0].toString().split("=")[1].replace("\"", "");

        Class<? extends Annotation> defaultEffect = genericEffect.checkClassType(teStr);
        return defaultEffect;
        //return genericEffect.getBottomMostEffectInLattice();
    }
    public Class<? extends Annotation> getDeclaredEffect(ExecutableElement methodElt, Element clsElt) {
        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect = null;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = getDeclAnnotation(methodElt, OkEffect);
            if (annotatedEffect != null) {
                if (debugSpew) {
                    System.err.println("Method marked @" + annotatedEffect);
                }
                return OkEffect;
            }
        }
        // this needs to be changed to return a different type depending on the type of the class
        //enclosing element may not get class level annotation
        String teStr = getClassType(clsElt);

        //this might not parse string safely
        //String effect = anno.getElementValues().entrySet().toArray()[0].toString().split("=")[1].replace("\"", "");

        Class<? extends Annotation> defaultEffect = genericEffect.checkClassType(teStr);
        return defaultEffect;
        //return genericEffect.getBottomMostEffectInLattice();
    }

    private String getClassType(Element clsElt)
    {
        //fix this to pass around objects instead of strings
        TypeMirror clsAnno = null;
        try {
            clsElt.getAnnotation(DefaultEffect.class).value();
        }
        catch(NullPointerException e)
        {
            return "SafeCast";
        }
        catch(MirroredTypeException e)
        {
            clsAnno = e.getTypeMirror();
        }

        if(clsAnno == null)
            return "SafeCast";
        //could possible shorten this or change checkClassType method
        Types TypeUtils = this.processingEnv.getTypeUtils();
        TypeElement te =  (TypeElement)TypeUtils.asElement(clsAnno);
        Name teName = te.getSimpleName();
        String teStr = teName.toString();
        return teStr;
    }









    /**
     * Returns the Declared Effect on the passed method as parameter
     *
     * @param methodElt : Method for which declared effect is to be returned
     * @return declared effect : if methodElt is annotated with a valid effect
     *     bottomMostEffectInLattice : otherwise, bottom most effect of lattice
     */
    public Class<? extends Annotation> getDeclaredEffect(ExecutableElement methodElt) {
        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect = null;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = getDeclAnnotation(methodElt, OkEffect);
            if (annotatedEffect != null) {
                if (debugSpew) {
                    System.err.println("Method marked @" + annotatedEffect);
                }
                return OkEffect;
            }
        }
        // this needs to be changed to return a different type depending on the type of the class
        //enclosing element may not get class level annotation
        AnnotationMirror anno = getDeclAnnotation(methodElt.getEnclosingElement(), DefaultEffect.class);

        if(anno == null || anno.getElementValues().entrySet().toArray().length == 0)
            return genericEffect.getBottomMostEffectInLattice();
        //this might not parse string safely
        String effect = anno.getElementValues().entrySet().toArray()[0].toString().split("=")[1].replace("\"", "");
        Class<? extends Annotation> defaultEffect = genericEffect.checkClassType(effect);
        return defaultEffect;
        //return genericEffect.getBottomMostEffectInLattice();
    }


    /**
     * Looks for invalid overrides, (cases where a method override declares a larger/higher effect
     * than a method it overrides/implements)
     *
     * <p>Process followed: Get the overriding method annotations Iterate over all of its subtypes
     * For each subtype, that has its own implementation or declaration of the input method: Check
     * that the effect of the override is less than the declared effect of the origin.
     *
     * <p>There are two sets of subtypes to traverse: 1. Chain of Parent classes terminating in
     * Object 2. Set of interfaces the class implements.
     *
     * @param declaringType : Class containing the overriding method
     * @param overridingMethod : Overriding method in declaringType
     * @param issueConflictWarning : true if warning should be issued
     * @param errorNode : node to check for errors
     */
    public void checkEffectOverride(
            TypeElement declaringType,
            ExecutableElement overridingMethod,
            boolean issueConflictWarning,
            Tree errorNode) {
        assert (declaringType != null);

        Class<? extends Annotation> overridingEffect = getDeclaredEffect(overridingMethod);

        // Chain of parent classes
        TypeMirror superclass = declaringType.getSuperclass();
        while (superclass != null && superclass.getKind() != TypeKind.NONE) {
            ExecutableElement overrides = findJavaOverride(overridingMethod, superclass);
            if (overrides != null) {
                Class<? extends Annotation> superClassEffect = getDeclaredEffect(overrides);
                if (!genericEffect.LE(overridingEffect, superClassEffect)) {
                    checker.report(
                            Result.failure(
                                    "override.effect.invalid",
                                    overridingMethod,
                                    declaringType,
                                    overrides,
                                    superclass),
                            errorNode);
                }
            }

            DeclaredType decl = (DeclaredType) superclass;
            superclass = ((TypeElement) decl.asElement()).getSuperclass();
        }

        // Set of interfaces
        List<? extends TypeMirror> listOfInterfaces = declaringType.getInterfaces();
        if (listOfInterfaces != null) {
            for (TypeMirror implementedInterface : listOfInterfaces) {
                if (implementedInterface.getKind() != TypeKind.NONE) {
                    ExecutableElement overrides =
                            findJavaOverride(overridingMethod, implementedInterface);
                    if (overrides != null) {
                        Class<? extends Annotation> interfaceEffect = getDeclaredEffect(overrides);
                        if (!genericEffect.LE(overridingEffect, interfaceEffect)
                                && issueConflictWarning) {
                            checker.report(
                                    Result.failure(
                                            "override.effect.invalid",
                                            overridingMethod,
                                            declaringType,
                                            overrides,
                                            implementedInterface),
                                    errorNode);
                        }
                    }
                }
            }
        }
    }
}
