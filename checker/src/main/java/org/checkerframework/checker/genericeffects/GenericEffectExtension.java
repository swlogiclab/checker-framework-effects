package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;

/**
 * Base class for effect system plugins, allowing a lightweight way for developers to indicate that
 * specific AST node should raise a certain custom effect. This makes it possible to ascribe effects
 * to the use of Java language features.
 *
 * <p>To ascribe effects to select instances of a particular tree type, override the <code>
 * doesXCheck()</code> and <code>checkX()</code> methods for each appropriate X.
 */
public class GenericEffectExtension<X> {

  /** Reference to the effect quantale */
  protected EffectQuantale<X> genericEffects;

  /**
   * Constructor to set the lattice.
   *
   * @param lattice The lattice of the checker.
   */
  public GenericEffectExtension(EffectQuantale<X> lattice) {
    genericEffects = lattice;
  }

  /**
   * These methods should be overridden in a new class depending on the type of checker the
   * developer is creating. Note: Mostly all trees are checked except for ones that have been judged
   * to be unhelpful or encompass too many things.
   *
   * @return A boolean value representing whether a check should take place (true) or not (false).
   */
  public boolean doesArrayAccessCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on array types.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesArrayTypeCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on assertions.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesAssertCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on assignments.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesAssignmentCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on binary operations.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesBinaryCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on break statements.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesBreakCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on a case.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesCaseCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on catch blocks.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesCatchCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on compound assignments.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesCompoundAssignmentCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on conditional (ternary)
   * expressions.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesConditionalExpressionCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on continue statements.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesContinueCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on do-while loops.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesDoWhileLoopCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on enhanced for loops.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesEnhancedForLoopCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on standard for loops.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesForLoopCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on conditional statements.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesIfCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on uses of <code>instanceof</code>.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesInstanceOfCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on uses of intersection types.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesIntersectionTypeCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on labeled statements.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesLabeledStatementCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on literals.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesLiteralCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on array allocations.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesNewArrayCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on primitive types.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesPrimitiveTypeCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on return statements.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesReturnCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on switch statements.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesSwitchCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on synchronized blocks.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesSynchronizedCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on throw statements.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesThrowCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on try blocks.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesTryCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on type casts.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesTypeCastCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on unary operations.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesUnaryCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on uses of union types.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesUnionTypeCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on while loops.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesWhileLoopCheck() {
    return false;
  }

  /**
   * Indicates whether this extension performs additional checks on uses of wildcard types.
   *
   * @return Whether this AST node is checked by this extension.
   */
  public boolean doesWildcardCheck() {
    return false;
  }

  /**
   * These methods should be overridden in a new class depending on the type of checker the
   * developer is creating. Note: Mostly all trees are checked except for ones that have been judged
   * to be unhelpful or encompass too many things.
   *
   * @param node The specific tree node that the developer wants to check.
   * @return The effect of the specific tree node or throws an UnsupportedOperationException if not
   *     overridden.
   */
  public X checkArrayAccess(ArrayAccessTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkArrayType(ArrayTypeTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkAssert(AssertTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkAssignment(AssignmentTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkBinary(BinaryTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkBreak(BreakTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkCase(CaseTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkCatch(CatchTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkCompoundAssignment(CompoundAssignmentTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkConditionalExpression(ConditionalExpressionTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkContinue(ContinueTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkDoWhileLoop(DoWhileLoopTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkEnhancedForLoop(EnhancedForLoopTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkForLoop(ForLoopTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkIf(IfTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkInstanceOf(InstanceOfTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkIntersectionType(IntersectionTypeTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkLabeledStatement(LabeledStatementTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkLiteral(LiteralTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkNewArray(NewArrayTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkPrimitiveType(PrimitiveTypeTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkReturn(ReturnTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkSwitch(SwitchTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkSynchronized(SynchronizedTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkThrow(ThrowTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkTry(TryTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkTypeCast(TypeCastTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkUnary(UnaryTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkUnionType(UnionTypeTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkWhileLoop(WhileLoopTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs extra checks (if appropriate) on this AST node.
   *
   * @param node Node to check
   * @return Representation of the effect this node should have, if other than unit.
   */
  public X checkWildcard(WildcardTree node) {
    throw new UnsupportedOperationException();
  }

  /**
   * This method should be overridden in a new class to define errors that should occur during
   * checking.
   *
   * @param node A tree node that represents all nodes that are being checked.
   * @return An error/failure message depending on the tree node.
   */
  public @CompilerMessageKey String reportError(Tree node) {
    return null;
  }

  /**
   * This method should be overridden in a new class to define warnings that should occur during
   * checking.
   *
   * @param node A tree node that represents all nodes that are being checked.
   * @return A warning message depending on the tree node.
   */
  public @CompilerMessageKey String reportWarning(Tree node) {
    return null;
  }

  /**
   * This method should not be overridden unless compiler arguments need to be parsed differently.
   *
   * @param compilerArgs Arguments that were taken from the compiler.
   * @param error Failure/Warning that needs to be checked to see if it was in the compiler
   *     arguments.
   * @return A boolean value if the value is in the compiler arguments (true) or not (false).
   */
  public boolean isIgnored(String compilerArgs, String error) {
    String[] parsedArgs = compilerArgs.split(",");
    for (String args : parsedArgs) {
      if (args.equals(error)) return true;
    }
    return false;
  }
}
