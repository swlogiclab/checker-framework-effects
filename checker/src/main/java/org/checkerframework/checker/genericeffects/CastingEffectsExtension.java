package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import java.lang.annotation.Annotation;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.genericeffects.qual.DecimalOverflow;
import org.checkerframework.checker.genericeffects.qual.DecimalPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.SafeCast;
import org.checkerframework.javacutil.TreeUtils;

public class CastingEffectsExtension extends GenericEffectExtension {

  /**
   * Constructor that takes the lattice in as a parameter and passes it to the constructor of the
   * superclass.
   *
   * @param lattice The effect lattice for the checker.
   */
  public CastingEffectsExtension(EffectQuantale<Class<? extends Annotation>> lattice) {
    super(lattice);
  }

  /**
   * Overridden method from GenericEffectExtension that activates type cast checking.
   *
   * @return A boolean value representing whether the type cast check is activated.
   */
  @Override
  public boolean doesTypeCastCheck() {
    return true;
  }

  /**
   * Overridden method from GenericEffectExtension that specifies the effects of type cast tree
   * nodes.
   *
   * @param node A type cast tree node that is encountered when checking the program.
   * @return The effect of node that is encountered.
   */
  @Override
  public Class<? extends Annotation> checkTypeCast(TypeCastTree node) {
    TypeKind castTo = TreeUtils.typeOf(node.getType()).getKind();
    TypeKind beingCast = TreeUtils.typeOf(node.getExpression()).getKind();
    if (isSafeLiteral(node)) return SafeCast.class;
    else if ((beingCast == TypeKind.LONG
            && (castTo == TypeKind.INT || castTo == TypeKind.SHORT || castTo == TypeKind.BYTE))
        || (beingCast == TypeKind.INT && (castTo == TypeKind.SHORT || castTo == TypeKind.BYTE))
        || (beingCast == TypeKind.SHORT && castTo == TypeKind.BYTE)) return IntegerOverflow.class;
    else if ((beingCast == TypeKind.DOUBLE || beingCast == TypeKind.FLOAT)
        && (castTo == TypeKind.LONG
            || castTo == TypeKind.INT
            || castTo == TypeKind.SHORT
            || castTo == TypeKind.BYTE)) return DecimalOverflow.class;
    else if ((beingCast == TypeKind.LONG && (castTo == TypeKind.DOUBLE || castTo == TypeKind.FLOAT))
        || (beingCast == TypeKind.INT && castTo == TypeKind.FLOAT))
      return IntegerPrecisionLoss.class;
    else if (beingCast == TypeKind.DOUBLE && castTo == TypeKind.FLOAT)
      return DecimalPrecisionLoss.class;
    else return SafeCast.class;
  }

  /**
   * Private method for checking if a type cast involving a literal is safe. Note: This method only
   * accounts for integer literals such as ints and longs. Floats, doubles, and chars are not
   * included due to their strange behavior/representation. TODO: Find a way to check float
   * literals.
   *
   * @param node A type cast tree node that is encountered while checking.
   * @return A boolean value representing whether the type cast tree node is safe for a casting
   *     involving literals.
   */
  private boolean isSafeLiteral(TypeCastTree node) {
    TypeMirror t = TreeUtils.typeOf(node.getType());
    if (node.getExpression().getKind() == Tree.Kind.INT_LITERAL) {
      int val = Integer.parseInt(node.getExpression().toString());
      if (t.getKind() == TypeKind.BYTE) {
        if (val > Byte.MAX_VALUE || val < Byte.MIN_VALUE) return false;
      } else if (t.getKind() == TypeKind.SHORT) {
        if (val > Short.MAX_VALUE || val < Short.MIN_VALUE) return false;
      }
      return true;
    } else if (node.getExpression().getKind() == Tree.Kind.LONG_LITERAL) {
      long val = Long.parseLong(node.getExpression().toString().replace("L", ""));
      if (t.getKind() == TypeKind.BYTE) {
        if (val > Byte.MAX_VALUE || val < Byte.MIN_VALUE) return false;
      } else if (t.getKind() == TypeKind.SHORT) {
        if (val > Short.MAX_VALUE || val < Short.MIN_VALUE) return false;
      } else if (t.getKind() == TypeKind.INT) {
        if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Overridden method that specifies which error to report depending on the nodes encountered while
   * checking. Note: This method does not take in a specific tree node; therefore, the tree node
   * must be cast to a specific tree node depending on which checks are active.
   *
   * @param node Nodes encountered for all active checks.
   * @return String object representing the failure message.
   */
  @Override
  public @CompilerMessageKey String reportError(Tree node) {
    if (node.getKind() == Tree.Kind.TYPE_CAST) return "cast.invalid";
    return null;
  }

  /**
   * Overridden method that specifies which warning to report depending on the nodes encountered
   * while checking. Note: This method does not take in a specific tree node; therefore, the tree
   * node must be cast to a specific tree node depending on which checks are active.
   *
   * @param node Nodes encountered for all active checks.
   * @return String object representing the warning message.
   */
  @Override
  public @CompilerMessageKey String reportWarning(Tree node) {
    if (node.getKind() == Tree.Kind.TYPE_CAST) {
      TypeCastTree typeCastNode = (TypeCastTree) node;
      TypeKind castTo = TreeUtils.typeOf(typeCastNode.getType()).getKind();
      TypeKind beingCast = TreeUtils.typeOf(typeCastNode.getExpression()).getKind();
      if (beingCast.isPrimitive()
          && beingCast != TypeKind.CHAR
          && beingCast != TypeKind.BOOLEAN
          && beingCast == castTo) return "cast.redundant";
    }
    return null;
  }
}
