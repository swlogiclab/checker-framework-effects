package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.naming.ldap.Control;

import org.checkerframework.checker.genericeffects.qual.DefaultEffect;
import org.checkerframework.checker.genericeffects.qual.Placeholder;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import static org.checkerframework.checker.genericeffects.ControlEffectQuantale.ControlEffect;
import static org.checkerframework.checker.genericeffects.ControlEffectQuantale.LocatedEffect;
import org.checkerframework.checker.genericeffects.qual.ThrownEffect;

/**
 * A base type factory for effect systems.
 *
 * <p>Provides a range of utilities for looking up and manipulating effects, including support for
 * automatically interpreting {@link DefaultEffect} annotations when determining method effects.
 */
public class GenericEffectTypeFactory<X> extends BaseAnnotatedTypeFactory {

  /** Whether to emit all debugging information. */
  protected final boolean debugSpew;

  /** Reference to the effect quantale being checked. */
  private ControlEffectQuantale<X> genericEffect;

  /**
   * Constructor for the checker's type factory.
   *
   * @param checker The checker object that allows the type factory to access the lattice of the
   *     checker.
   * @param spew Boolean used for debugging.
   */
  public GenericEffectTypeFactory(GenericEffectChecker<X> checker, boolean spew) {
    // use true to enable flow inference, false to disable it
    super(checker, false);

    genericEffect = new ControlEffectQuantale<>(checker.getEffectLattice());

    debugSpew = spew;
    this.postInit();
  }

  public void setConversion(Function<Class<? extends Annotation>, X> fromAnno) {
    assert (fromAnno != null);
    fromAnnotation = fromAnno;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    HashSet<Class<? extends Annotation>> hs = new HashSet<>();
    hs.add(Placeholder.class);
    return hs;
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
   * This method is used to get the inner most class with a DefaultEffect annotation from an element
   * (in case there are nested classes). This is done by continually getting the enclosing element
   * of an element until the desired annotation is found. Note: If the developer would not like this
   * feature. The commented code at the bottom will only return the enclosing class of the element
   * and ignore nested classes.
   *
   * @param elt An element for which a DefaultEffect annotated class has to be found.
   * @return Inner most annotated class with the DefaultEffect annotation. If none is found then the
   *     outer most class is returned.
   */
  private Element getInnermostAnnotatedClass(Element elt) {
    Element encElt = elt;
    while (encElt != null) {
      if (encElt.getAnnotation(DefaultEffect.class) != null) return encElt;
      else encElt = encElt.getEnclosingElement();
    }
    return elt;
    /*
    while (encElt.getKind() != ElementKind.CLASS || encElt.getKind() != ElementKind.INTERFACE) {
        encElt = encElt.getEnclosingElement();
    }
    return encElt
    */
  }

  private Function<Class<? extends Annotation>, X> fromAnnotation;

  /**
   * This method is used to get the default effect of a class that is annotated with DefaultEffect.
   * The way this method works is by attempting to call the value() method of DefaultEffect which
   * will raise a mirrored type exception because it is "attempting to access a class object
   * corresponding to a TypeMirror". The current solution to this is to analyze the exception and
   * get the TypeMirror object because it contains the information that is needed. Note: This link
   * provides more information on this workaround and may be useful for future changes:
   * https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation
   *
   * @param clsElt An element representing a class.
   * @return The default effect of the class element that was passed as a parameter.
   */
  private X getClassType(Element clsElt) {
    // TODO: There may be a better approach to getting the information that is needed than
    // raising an exception
    TypeMirror clsAnno = null;
    try {
      clsElt.getAnnotation(DefaultEffect.class).value();
    } catch (NullPointerException e) {
      return genericEffect.underlyingUnit();
    } catch (MirroredTypeException e) {
      clsAnno = e.getTypeMirror();
    }
    // TODO: Find a way to extract the class type from the TypeElement object without making use
    // of Strings.
    Types TypeUtils = this.processingEnv.getTypeUtils();
    TypeElement typeElt = (TypeElement) TypeUtils.asElement(clsAnno);
    String name = typeElt.getSimpleName().toString();
    for (Class<? extends Annotation> validEffect : genericEffect.getValidEffects()) {
      if (name.equals(validEffect.getSimpleName())) return this.fromAnnotation.apply(validEffect);
    }
    return genericEffect.underlyingUnit();
  }

  /**
   * Method that gets the default effect of a element by looking at its class annotation.
   *
   * @param elt Element for which the default effect will be retrieved.
   * @return Default effect of the element.
   */
  public X getDefaultEffect(Element elt) {
    Element clsElt = getInnermostAnnotatedClass(elt);
    return getClassType(clsElt);
  }

  /**
   * Returns the Declared Effect on the passed method as parameter
   *
   * @param methodElt : Method for which declared effect is to be returned
   * @param use : The tree where this element is being invoked
   * @return declared effect : if methodElt is annotated with a valid effect
   *     bottomMostEffectInLattice : otherwise, bottom most effect of lattice
   */
  public ControlEffect<X> getDeclaredEffect(ExecutableElement methodElt, Tree use) {
    if (debugSpew) {
      System.err.println("> Retrieving declared effect of: " + methodElt);
    }
    ArrayList<Class<? extends Annotation>> validEffects = genericEffect.getValidEffects();
    AnnotationMirror annotatedEffect = null;
    X baseEffect = null;

    for (Class<? extends Annotation> OkEffect : validEffects) {
      annotatedEffect = getDeclAnnotation(methodElt, OkEffect);
      if (annotatedEffect != null) {
        if (debugSpew) {
          System.err.println("< Method marked @" + annotatedEffect);
        }
        baseEffect = fromAnnotation.apply(OkEffect);
      }
    }

    if (baseEffect == null) {
      Element clsElt = getInnermostAnnotatedClass(methodElt);
      if (debugSpew) {
        System.err.println("< By default found: " + getClassType(clsElt));
      }
      baseEffect = getClassType(clsElt);
    }

    // We have a base effect, now check for @Throws annotations
    Map<Class<?>, Set<LocatedEffect<X>>> excBehaviors = new HashMap<>();
    // Check that any @ThrownEffect uses are valid
    for (AnnotationMirror thrown : getDeclAnnotations(methodElt)) {
      if (areSameByClass(thrown, ThrownEffect.class)) {
        ThrownEffect thrownEff = (ThrownEffect) thrown;
        // TODO: require the effect be a checked exception (i.e., not subtype of RuntimeException)
        excBehaviors.put(thrownEff.exception(), 
                             Collections.singleton(new LocatedEffect<X>(fromAnnotation.apply(thrownEff.behavior()), use)));
      }
    }

    return new ControlEffectQuantale.ControlEffect<X>(baseEffect, excBehaviors.size() > 0 ? excBehaviors : null, null);
  }

  /**
   * Looks for invalid overrides, (cases where a method override declares a larger/higher effect
   * than a method it overrides/implements)
   *
   * <p>Process followed: Get the overriding method annotations Iterate over all of its subtypes For
   * each subtype, that has its own implementation or declaration of the input method: Check that
   * the effect of the override is less than the declared effect of the origin.
   *
   * <p>There are two sets of subtypes to traverse: 1. Chain of Parent classes terminating in Object
   * 2. Set of interfaces the class implements.
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

    ControlEffect<X> overridingEffect = getDeclaredEffect(overridingMethod, errorNode);

    // Chain of parent classes
    TypeMirror superclass = declaringType.getSuperclass();
    while (superclass != null && superclass.getKind() != TypeKind.NONE) {
      ExecutableElement overrides = findJavaOverride(overridingMethod, superclass);
      if (overrides != null) {
        ControlEffect<X> superClassEffect = getDeclaredEffect(overrides, errorNode);
        if (!genericEffect.LE(overridingEffect, superClassEffect)) {
          checker.reportError(
              errorNode,
              "override.effect.invalid",
              overridingMethod,
              declaringType,
              overrides,
              superclass);
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
          ExecutableElement overrides = findJavaOverride(overridingMethod, implementedInterface);
          if (overrides != null) {
            ControlEffect<X> interfaceEffect = getDeclaredEffect(overrides, errorNode);
            if (!genericEffect.LE(overridingEffect, interfaceEffect) && issueConflictWarning) {
              checker.reportError(
                  errorNode,
                  "override.effect.invalid",
                  overridingMethod,
                  declaringType,
                  overrides,
                  implementedInterface);
            }
          }
        }
      }
    }
  }
}
