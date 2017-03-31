package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;

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
        return genericEffect.getBottomMostEffectInLattice();
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
