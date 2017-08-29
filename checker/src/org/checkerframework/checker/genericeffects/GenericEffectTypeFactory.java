package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;

public class GenericEffectTypeFactory extends BaseAnnotatedTypeFactory {

    protected final boolean debugSpew;
    private GenericEffectLattice genericEffect;

    /**
     * Constructor for the checker's type factory.
     *
     * @param checker The checker object that allows the type factory to access the lattice of the checker.
     * @param spew Boolean used for debugging.
     */
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

    /**
     * This method is used to get the inner most class with a DefaultEffect annotation from an element (in case there are nested classes).
     * Note: If the developer would not like this feature. The commented code at the bottom will only return the enclosing class of the element
     * and ignore nested classes.
     *
     * @param elt An element for which a DefaultEffect annotated class has to be found.
     * @return Inner most annotated class with the DefaultEffect annotation. If none is found then the outer most class is returned.
     */
    private Element getInnermostAnnotatedClass(Element elt) {
        Element clsElt = elt;
        while (clsElt != null) {
            if (clsElt.getAnnotation(DefaultEffect.class) != null)
                break;
            else
                clsElt = clsElt.getEnclosingElement();
        }
        return clsElt;
       /*
       while(clsElt.getKind() != ElementKind.CLASS) {
           clsElt = clsElt.getEnclosingElement();
       }
       return clsElt;
       */
    }

    /**
     * This method is used to get the default effect of a class that is annotated with DefaultEffect.
     * The way this method works is by attempting to call the value() method of DefaultEffect which will
     * raise a mirrored type exception because it is "attempting to access a class object corresponding to
     * a TypeMirror". The current solution to this is to analyze the exception and get the TypeMirror object
     * because it contains the information that is needed.
     *
     * @param clsElt An element representing a class.
     * @return The default effect of the class element that was passed as a parameter.
     */
    //https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation
    private Class<? extends Annotation> getClassType(Element clsElt)
    {
        //TODO: There may be a better approach to getting the information that is needed than raising an exception
        TypeMirror clsAnno = null;
        try {
            clsElt.getAnnotation(DefaultEffect.class).value();
        }
        catch(NullPointerException e)
        {
            return genericEffect.getBottomMostEffectInLattice();
        }
        catch(MirroredTypeException e)
        {
            clsAnno = e.getTypeMirror();
        }
        //TODO: Find a way to extract the class type from the TypeElement object without making use of Strings.
        Types TypeUtils = this.processingEnv.getTypeUtils();
        TypeElement typeElt =  (TypeElement)TypeUtils.asElement(clsAnno);
        String name = typeElt.getSimpleName().toString();
        for(Class<? extends Annotation> validEffect : genericEffect.getValidEffects())
        {
            if(name.equals(validEffect.getSimpleName()))
                return validEffect;
        }
        return genericEffect.getBottomMostEffectInLattice();
    }


    /**
     * Returns the Declared Effect on the passed element as parameter
     *
     * @param elt : Element for which declared effect is to be returned
     * @return declared effect : if methodElt is annotated with a valid effect
     *     bottomMostEffectInLattice : otherwise, bottom most effect of lattice
     */
    public Class<? extends Annotation> getDeclaredEffect(Element elt)
    {
        ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
        AnnotationMirror annotatedEffect = null;

        for (Class<? extends Annotation> OkEffect : validEffects) {
            annotatedEffect = getDeclAnnotation(elt, OkEffect);
            if (annotatedEffect != null) {
                if (debugSpew) {
                    System.err.println("Method marked @" + annotatedEffect);
                }
                return OkEffect;
            }
        }

        Element clsElt = getInnermostAnnotatedClass(elt);
        return getClassType(clsElt);
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

        Element clsElt = getInnermostAnnotatedClass(methodElt);
        return getClassType(clsElt);
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
