package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
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
import com.sun.tools.javac.code.Type.ClassType;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle.Control;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.genericeffects.ControlEffectQuantale.NonlocalEffect;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * GenericEffectVisitor is a base class for effect systems, including sequential effect systems.
 *
 * <p>The general idea is for checking the effect of a method to proceed by initializing an
 * accumulator to the specific system's unit effect ({@link EffectQuantale#unit()}), then
 * recursively traverse the AST to accumulate the overall effect of various subtrees. Each visit
 * method should leave the accumulator holding the effect of (only) the AST node visited.
 *
 * <p>Because methods can actually nest (if a method includes an anonymous inner class with a method
 * definition), we actually keep a <i>stack</i> of accumulators. The top element of the stack is the
 * accumulator for the current context.
 *
 * <p>The stack depth changes whenever a new AST node is visited. Upon entry to any visit method,
 * the top-most element of the stack should contain the effect of the method so far (i.e., the
 * preceeding context effect), which will aid (with further extension) in precise error reporting
 * for sequential effect systems. Upon exit, the method should leave the stack the same depth as
 * upon entry..... HMM, this is getting messy, there's a tension here between updating the stack
 * consistently so subexpressions can give good error reporting, and keeping things separate so
 * non-sequential composition (e.g., conditionals/switch) can compute the right upper bound to leave
 * for the next thiing.... or maybe snapshotting and exploring all paths is enough due to distrib
 * laws? Except for loops... and then exceptions, no we need modular tracking to assign effects to
 * individual subexpressions. So maybe we need a stack of stacks, error reporting from accumulating
 * across top-most stack :-p TODO: Add methods to the GenericEffectChecker to configure default
 * upper bounds on static and instance field initializers (static runs anywhere, field runs with
 * <i>every</i> ctor).
 */
@SuppressWarnings("interning:not.interned")
public class GenericEffectVisitor<X> extends BaseTypeVisitor<GenericEffectTypeFactory<X>> {

  /** Debug flag, set via the "debugSpew" lint option */
  protected final boolean debugSpew;
  /** Reference to the effect quantale being checked. */
  private ControlEffectQuantale<X> genericEffect;
  /** Reference to a plugin for determining the effects of basic Java language features. */
  private GenericEffectExtension<X> extension;
  /** Flag to disable residual checking for systems that may not yet support it */
  private final boolean noResiduals;

  /**
   * A stack of effect contexts, one for each level of nested methods (to support anonymous inner
   * classes).
   */
  protected final Deque<ContextEffect<ControlEffectQuantale<X>.ControlEffect>> effStack;
  /**
   * A stack of references to the methods being processed, including null for field initialization
   * and static initializer blocks.
   */
  protected final Deque<MethodTree> currentMethods;
  /**
   * A stack of residual targets for residual checking --- i.e., the effect that effect checking is
   * trying to "complete"
   */
  protected final Deque<ControlEffectQuantale<X>.ControlEffect> residualTargets;

  /** Flag to disable effect checking */
  boolean ignoringEffects;
  /** Flag to disable warnings */
  boolean ignoringWarnings;
  /** Flag to disable errors */
  boolean ignoringErrors;

  /** Flag indicating whether the current path has already reported a type error. */
  boolean errorOnCurrentPath;

  /** Specialized reference to type factory that knows the representation type of effects. */
  GenericEffectTypeFactory<X> xtypeFactory;

  GenericEffectChecker<X> xchecker;

  @SuppressWarnings("UnusedVariable")
  private Function<Class<? extends Annotation>, X> fromAnnotation;

  /**
   * Constructor that takes passes the checker to the superclass and takes in a
   * GenericEffectExtension object. The effect stack for methods and variables are set up in the
   * constructor.
   *
   * @param checker The checker that allows the Casting Effects Checker to function.
   * @param ext An GenericEffectExtension object that provides the developer with more functions
   *     dealing with specific tree nodes.
   */
  @SuppressWarnings("JdkObsolete")
  public GenericEffectVisitor(
      GenericEffectChecker<X> checker,
      GenericEffectExtension<X> ext,
      Function<Class<? extends Annotation>, X> fromAnno) {
    super(checker);
    xchecker = checker;
    debugSpew = checker.getLintOption("debugSpew", false);

    fromAnnotation = fromAnno;
    xtypeFactory.setConversion(fromAnno);

    /* ErrorProne JdkObsolete warnings are suppressed here because we must use a deque/stack implementation that permits null.
     * Without supressing this warning, ErrorProne complains we should be using ArrayDeque, which rejects null elements. */
    effStack = new LinkedList<ContextEffect<ControlEffectQuantale<X>.ControlEffect>>();
    currentMethods = new LinkedList<MethodTree>();
    residualTargets = new LinkedList<ControlEffectQuantale<X>.ControlEffect>();

    extension = ext;

    ignoringEffects = checker.getOption("ignoreEffects") != null;
    ignoringWarnings = checker.getOption("ignoreWarnings") != null;
    ignoringErrors = checker.getOption("ignoreErrors") != null;
    errorOnCurrentPath = false;

    genericEffect = new ControlEffectQuantale<X>(checker.getEffectLattice(), xtypeFactory);
    noResiduals = !checker.getEffectLattice().supportsErrorLocalization();

    if (debugSpew) {
      System.err.println(
          "Loading generic effect visitor with effect quantale: "
              + genericEffect.getClass().toString());
      System.err.println(
          "Loading generic effect visitor with type factory: "
              + xtypeFactory.getClass().toString());
    }
  }

  /**
   * Method to instantiate the factory class for the checker.
   *
   * @return The type factory of the checker.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected GenericEffectTypeFactory<X> createTypeFactory() {
    // This unchecked case is present because this method is invoked from the supertype's
    // constructor, so only checker (as BaseTypeChecker) is set. But since it's set from a ctor
    // argument that must be a GenericEffectChecker with appropriate effect representation, this is
    // always correct.
    // This is called from the super constructor, so xchecker is not yet set
    xtypeFactory =
        new GenericEffectTypeFactory<X>(
            (GenericEffectChecker<X>) checker, checker.getLintOption("debugSpew", false));
    return xtypeFactory;
  }

  /**
   * TODO: Please document the use off this with respect to the generic effect checker better. Note:
   * The GuiEffectChecker uses a similar setup and provides more documentation.
   *
   * @param node Class declaration to process
   */
  @Override
  public void processClassTree(ClassTree node) {
    // Fix up context for static initializers of new class
    currentMethods.addFirst(null);
    effStack.addFirst(new ContextEffect<ControlEffectQuantale<X>.ControlEffect>(genericEffect));
    residualTargets.addFirst(null);
    super.processClassTree(node);
    currentMethods.removeFirst();
    effStack.removeFirst();
    residualTargets.removeFirst();
  }

  /**
   * Method that visits method tree nodes and adds their effects to the stacks set up in the
   * constructor.
   *
   * @param node The method tree node that was encountered during checking.
   * @param p Void
   * @return Void
   */
  @Override
  public Void visitMethod(MethodTree node, Void p) {
    // Save and restore errorOnCurrentPath, so methods of anonymous inner classes
    // don't inherit contextual errors from their allocating contexts
    boolean contextualErrorOnCurrentPath = errorOnCurrentPath;
    errorOnCurrentPath = false;

    ExecutableElement methElt = TreeUtils.elementFromDeclaration(node);
    if (debugSpew) {
      System.err.println("\nVisiting method " + methElt);
    }

    assert (methElt != null);

    // Override check
    xtypeFactory.checkEffectOverride(
        (TypeElement) methElt.getEnclosingElement(), methElt, true, node);

    // Initialize method stack
    currentMethods.addFirst(node);
    effStack.addFirst(new ContextEffect<>(genericEffect));
    residualTargets.addFirst(
        xtypeFactory.getDeclaredEffect(
            TreeUtils.elementFromDeclaration(currentMethods.peek()), node));

    if (debugSpew) {
      System.err.println(
          "Pushing " + effStack.peekFirst() + " onto the stack when checking " + methElt);
    }

    Void ret = super.visitMethod(node, p);

    // Completion Check
    // We skip this if every path to the end of the method already reported a type (effect) error
    // TODO: This isn't *quite* what we want for *commutative* effect systems, for which we'd like
    // to report *all* errors...
    // TODO: Maybe an extra flag on the lattice, so we handle non-comm differently from comm
    // systems?
    // TODO: Work out laws for residuals w/ comm: e.g., x\(y\z) def <-> y\(x\z) def?
    if (!errorOnCurrentPath) {
      ControlEffectQuantale<X>.ControlEffect targetEffect = effStack.peek().currentPathEffect();
      ControlEffectQuantale<X>.ControlEffect callerEffect =
          xtypeFactory.getDeclaredEffect(methElt, node);
      if (!effStack.peek().currentlyImpossible() && isInvalid(targetEffect, callerEffect))
        checkError(node, targetEffect, callerEffect, "subeffect.invalid.methodbody");
      else if (!effStack.peek().currentlyImpossible() && extension.reportWarning(node) != null)
        checkWarning(node, targetEffect, callerEffect, extension.reportWarning(node));
    }

    currentMethods.removeFirst();
    effStack.removeFirst();
    residualTargets.removeFirst();
    if (debugSpew) {
      System.err.println("Finished visiting method " + methElt + "\n");
    }

    errorOnCurrentPath = contextualErrorOnCurrentPath;

    return ret;
  }

  /**
   * Method that can be used in a visitor method to see if a node is enclosed by a method.
   *
   * @return A boolean representing whether the node is enclosed by a method (true) or not (false).
   */
  @SuppressWarnings("UnusedMethod")
  private boolean hasEnclosingMethod() {
    MethodTree callerTree = TreePathUtil.enclosingMethod(getCurrentPath());
    return callerTree != null;
  }

  /**
   * Method to check is a target effect and a caller effect are invalid according to the lattice.
   * The method also checks which effects are to be ignored.
   *
   * @param targetEffect Target effect of node.
   * @param callerEffect Caller effect of node.
   * @return Boolean value representing whether the effects are invalid (true) or not (false)
   */
  private boolean isInvalid(
      ControlEffectQuantale<X>.ControlEffect targetEffect,
      ControlEffectQuantale<X>.ControlEffect callerEffect) {
    if (!genericEffect.LE(targetEffect, callerEffect)) {
      if (debugSpew) {
        System.err.println("Found\n\t" + targetEffect + "\n\t</=\n\t" + callerEffect);
        System.err.println("LUB=" + genericEffect.LUB(targetEffect, callerEffect));
      }
      return true;
    }
    return false;
  }

  /**
   * Method that reports an error as specified by given parameters. The method also checks which
   * errors are to be ignored.
   *
   * @param node Node for which error should be reported.
   * @param targetEffect Target effect of node.
   * @param callerEffect Caller effect of node.
   * @param failureMsg Error message to be reported.
   */
  private void checkError(
      Tree node,
      ControlEffectQuantale<X>.ControlEffect targetEffect,
      ControlEffectQuantale<X>.ControlEffect callerEffect,
      @CompilerMessageKey String failureMsg) {
    if (!ignoringErrors) checker.reportError(node, failureMsg, targetEffect, callerEffect);
    else if (ignoringErrors && !extension.isIgnored(checker.getOption("ignoreErrors"), failureMsg))
      checker.reportError(node, failureMsg, targetEffect, callerEffect);
  }

  /**
   * Internal method to check that the effect of the method body so far has not already become
   * incompatible with the annotated/assumed method effect. This is the core of reporting localized
   * error messages rather than method-global complaints, allowing us to report the earliest
   * location in program order where the method behaves incompatibly with what it should do.
   *
   * @param node The AST node currently being checked, which will be used as a possible error
   *     location.
   */
  private void checkResidual(Tree node) {
    // Not all effect quantales will support residuals
    if (noResiduals) {
      return;
    }

    // TODO: Impose actual checks on static & instance field initializer expression effects
    if (currentMethods.peek() == null) {
      return;
    }
    // Skip the check if we've already reported an error on this path.
    if (!ignoringErrors && !errorOnCurrentPath) {
      ControlEffectQuantale<X>.ControlEffect pathEffect = effStack.peek().currentPathEffect();
      // if (pathEffect == Impossible.class) {
      //  return; // In an enclosing context of a path that always throws/returns
      // }
      ControlEffectQuantale<X>.ControlEffect methodEffect = residualTargets.peek();

      // TODO: In general we don't want to check the residual against the method declaration,
      // because entering a break target or try-catch can change the acceptable behaviors. In
      // particular, inside a try-catch block we want to *almost* check residual w.r.t. the method
      // declaration, except for the exceptions caught locally, it's okay if sofar throws that
      // exception, as long as its throw-prefix has a residual with either the underlying effect or
      // some escaping effect of the method.

      if (debugSpew) {
        System.err.println("Checking residual " + pathEffect + " \\ " + methodEffect);
        // System.err.println("In location " + TreePathUtil.toString(visitorState.getPath()));
      }
      if (genericEffect.residual(pathEffect, methodEffect) == null) {
        if (genericEffect.isCommutative()) {
          // For commutative systems, we clean up the error message by peeking at the last addition
          checker.reportError(
              node, "operation.invalid", effStack.peek().latestEffect(), methodEffect);
          // For commutative systems, we *don't* set the current path error flag, but do reset the
          // accumulator so future residual checks *also* yield errors (we know they should since
          // sequencing is commutative)
          effStack.peek().rewriteLastEffectToCommutativeUnit();
        } else {
          checker.reportError(node, "undefined.residual", pathEffect, methodEffect);
          if (debugSpew) {
            System.err.println(
                "Residual " + pathEffect + "\\" + methodEffect + " was undefined!!!");
          }
          errorOnCurrentPath = true;
        }
      }
    }
  }

  /**
   * Method that reports a warning as specified by the given parameters. The method also checks
   * which warnings are to be ignored.
   *
   * @param node Node for which warning should be reported.
   * @param targetEffect Target effect of node.
   * @param callerEffect Caller effect of node.
   * @param warningMsg Warning message to be reported.
   */
  private void checkWarning(
      Tree node,
      ControlEffectQuantale<X>.ControlEffect targetEffect,
      ControlEffectQuantale<X>.ControlEffect callerEffect,
      @CompilerMessageKey String warningMsg) {
    if (!ignoringWarnings) checker.reportWarning(node, warningMsg, targetEffect, callerEffect);
    else if (ignoringWarnings
        && !extension.isIgnored(checker.getOption("ignoreWarnings"), warningMsg))
      checker.reportWarning(node, warningMsg, targetEffect, callerEffect);
  }

  /**
   * Method that is used by visitor methods to get the effect of a method that a node is within.
   *
   * @return Effect of a method that a node is within.
   */
  private ControlEffectQuantale<X>.ControlEffect getMethodCallerEffect() {
    MethodTree callerTree = TreePathUtil.enclosingMethod(getCurrentPath());
    ExecutableElement callerElt = TreeUtils.elementFromDeclaration(callerTree);
    return xtypeFactory.getDeclaredEffect(callerElt, callerTree);
  }

  /**
   * Retrieve the tree of the nearest enclosing break scope
   *
   * @return the tree for the nearest enclosing break scope
   */
  public Tree getEnclosingBreakScopeTree() {
    // Per docs, iterates from leaves to root
    for (Tree t : getCurrentPath()) {
      if (t.getKind() == Tree.Kind.SWITCH
          || t.getKind() == Tree.Kind.WHILE_LOOP
          || t.getKind() == Tree.Kind.DO_WHILE_LOOP
          || t.getKind() == Tree.Kind.FOR_LOOP
          || t.getKind() == Tree.Kind.ENHANCED_FOR_LOOP) {
        return t;
      }
    }
    return null;
  }

  /**
   * Retrieve the tree of where the specified exception type will be thrown or escape. Always
   * returns a {@code TryTree}, a {@code MethodTree}, or {@code null}.
   *
   * @param thrown The representation of the exception whose catch we'd like to find
   * @return Enclosing try-catch or method AST node, or null for initializer expressions
   */
  public Tree getEnclosingThrowScopeTree(ClassType thrown) {
    // Per docs, iterates from leaves to root
    for (Tree t : getCurrentPath()) {
      if (t.getKind() == Tree.Kind.METHOD) {
        return t;
      } else if (t.getKind() == Tree.Kind.TRY) {
        TryTree node = (TryTree) t;
        List<? extends CatchTree> catches = node.getCatches();
        for (CatchTree ct : catches) {
          // TODO: get mirror from
          // TreeUtils.elementfFromDeclaration(VariableTree) could apply to ct.getParameter()
          // if (ct.getParameter().getType())
          // Element e = TreeUtils.elementFromDeclaration(ct.getParameter());
          // assert (e != null);
          ClassType classty = (ClassType) TreeUtils.typeOf(ct.getParameter());

          // TODO: Figure out how to check supertypes for catches
          // TypeMirror upcast  TypesUtils.asSuper(thrown, classty, ???)
          if (atypeFactory.getProcessingEnv().getTypeUtils().isSubtype(thrown, classty)) {
            // if (TypesUtils.areSameDeclaredTypes(thrown, classty)) {
            return t;
          }
        }
      }
    }
    return null;
  }

  /**
   * Method that is used in a visitor method to get the default effect a class that a node is
   * within.
   *
   * <p>TODO: Must split between static vs. instance field initializers
   *
   * @return The default effect of a class that a node is within.
   */
  @SuppressWarnings("UnusedMethod")
  private X getDefaultClassEffect() {
    ClassTree clsTree = TreePathUtil.enclosingClass(getCurrentPath());
    Element clsElt = TreeUtils.elementFromDeclaration(clsTree);
    return xtypeFactory.getDefaultEffect(clsElt);
  }

  /**
   * Adapt a method (control) effect to a particular context. In particular, raw method effects have
   * exceptions targeting the method declaration itself (a correct approximation when checking the
   * method body), and this method relabels the nonlocal effects from exceptions to target an
   * appropriate try-catch block (if caught locally) or the enclosing method (if some exceptions
   * from the method effect would escape).
   *
   * @param contextless The declaration-side method effect
   * @return A method effect in the context of the current visitor state
   */
  private ControlEffectQuantale<X>.ControlEffect contextualize(
      ControlEffectQuantale<X>.ControlEffect contextless) {
    assert (contextless.breakset == null)
        : "Should never need to contextualize an effect with break behaviors, only method"
            + " declaration effects";
    if (contextless.excs == null) return contextless;
    X ul = contextless.base;
    // Rewrite every exception target from null to the nearest exception context
    Set<Pair<ClassType, NonlocalEffect<X>>> m = new HashSet<>();
    for (Pair<ClassType, NonlocalEffect<X>> e : contextless.excs) {
      m.add(
          Pair.of(
              e.first,
              new NonlocalEffect<>(
                  e.second.effect, getEnclosingThrowScopeTree(e.first), e.second.src)));
    }
    return genericEffect.new ControlEffect(ul, m, null);
  }

  /**
   * Method that visits all the method invocation tree nodes and raises failures/warnings for unsafe
   * method invocations.
   *
   * @param node Method invocation tree node that is found during checking.
   * @param p Void
   * @return Void
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    int contextSize = effStack.peek().size();
    // TODO extension checks
    // Set marker for scoping current
    effStack.peek().mark();
    scan(node.getMethodSelect(), p);
    for (Tree args : node.getArguments()) {
      scan(args, p);
    }
    ExecutableElement methodElt = TreeUtils.elementFromUse(node);
    ControlEffectQuantale<X>.ControlEffect targetEffect =
        contextualize(xtypeFactory.getDeclaredEffect(methodElt, node));
    if (debugSpew) {
      System.err.println("Pushing latent effect " + targetEffect + " for " + node);
    }
    boolean worked = effStack.peek().pushEffect(targetEffect, node);
    if (!worked) {
      errorOnCurrentPath = true;
      checker.reportError(
          node, "undefined.sequencing", effStack.peek().currentPathEffect(), targetEffect);
      // TODO: turn these detailed logs into a set of more detailed error messages
      genericEffect.lastSequencingErrors(); // resets per-operation error log
    }
    if (debugSpew) {
      System.err.println(
          "Path effect after " + node + " BEFORE squash is " + effStack.peek().currentPathEffect());
    }
    effStack.peek().squashMark(node);
    if (debugSpew) {
      System.err.println(
          "Path effect after " + node + " AFTER squash is " + effStack.peek().currentPathEffect());
      // System.err.println("In location " + TreePathUtil.toString(visitorState.getPath()));
      // System.err.println(
      //    "Static scope? " + TreePathUtil.isTreeInStaticScope(visitorState.getPath()));
      // TODO: Leaving this here as a reminder to find a new way to identify static scope
    }
    checkResidual(node);
    assert (effStack.peek().size() == 1 + contextSize);
    return p;
  }

  /**
   * Method to compute the effect of a variable access.
   *
   * <p>This might seem unnecessary, but many parts of this visitor assume visiting any tree pushes
   * some kind of effect onto the stack.
   */
  @Override
  public Void visitIdentifier(IdentifierTree node, Void p) {
    effStack.peek().pushEffect(genericEffect.unit(), node);
    // No need to check anything, we just pushed unit
    return p;
  }

  /**
   * Method to check if the constructor call is made from a valid context.
   *
   * <p>TODO: Fix for static vs. instance initializers
   *
   * @param node New class tree node that is found during checking.
   * @param p Void
   * @return Void
   */
  @Override
  public Void visitNewClass(NewClassTree node, Void p) {
    // TODO extension checks
    // Set marker for scoping current
    effStack.peek().mark();
    // for (Tree args : node.getArguments()) {
    //  scan(args, p);
    // }
    // Visit arguments, and if anonymous inner class, the inner class body
    super.visitNewClass(node, p);
    ExecutableElement methodElt = TreeUtils.elementFromUse(node);
    ControlEffectQuantale<X>.ControlEffect targetEffect =
        contextualize(xtypeFactory.getDeclaredEffect(methodElt, node));
    effStack.peek().pushEffect(targetEffect, node);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  /**
   * The methods below this comment follow the same format. Each method is a different visit method
   * for a different kind of tree node. Using the extensions class the developer can activate
   * specific visitor methods depending on what they want to check.
   *
   * <p>The methods work by first checking if the node being checked is enclosed by a method. If it
   * is then the method obtains the effect of the node and checks it against the method's effect. If
   * the node is not enclosed by a method, then it checks at the variable level against the class
   * annotation.
   *
   * @param node Specific tree node that is to be checked.
   * @param p Void
   * @return Void
   */
  @Override
  public Void visitArrayAccess(ArrayAccessTree node, Void p) {
    // TODO extension checks
    // Set marker for scoping current
    effStack.peek().mark();
    scan(node.getExpression(), p);
    scan(node.getIndex(), p);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitArrayType(ArrayTypeTree node, Void p) {
    if (extension.doesArrayTypeCheck()) {
      effStack.peek().pushEffect(genericEffect.lift(extension.checkArrayType(node)), node);
      checkResidual(node);
    }
    return super.visitArrayType(node, p);
  }

  @Override
  public Void visitAssert(AssertTree node, Void p) {
    // TODO extension checks
    // Assertions may or may not execute, so ensure either possibility is acceptable
    effStack.peek().mark();
    scan(node.getCondition(), p);
    ControlEffectQuantale<X>.ControlEffect condEff = effStack.peek().squashMark(node);
    ControlEffectQuantale<X>.ControlEffect joinWithUnit =
        genericEffect.LUB(genericEffect.unit(), condEff);
    if (joinWithUnit == null) {
      checker.reportError(node, "undefined.join.assertion", condEff);
    }
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void p) {
    // TODO extension checks
    // Set marker for scoping current
    effStack.peek().mark();
    scan(node.getVariable(), p);
    scan(node.getExpression(), p);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitBinary(BinaryTree node, Void p) {
    // TODO extension checks
    // Set marker for scoping current
    effStack.peek().mark();
    scan(node.getLeftOperand(), p);
    scan(node.getRightOperand(), p);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitBreak(BreakTree node, Void p) {
    if (node.getLabel() != null) {
      throw new UnsupportedOperationException(
          "Generic Effect Framework does not yet support labeled breaks");
    }
    effStack.peek().mark();
    effStack.peek().pushEffect(genericEffect.breakout(getEnclosingBreakScopeTree(), node), node);
    // TODO extension
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitCase(CaseTree node, Void p) {
    // TODO: need extra plumbing to be sound w.r.t. fallthrough

    effStack.peek().mark();
    // TODO: JDK 14 deprecated getExpression in favor of getExpressions (plural) for multi-label
    // cases.
    scan(node.getExpression(), p);
    scan(node.getStatements(), p);
    effStack.peek().squashMark(node);
    // TODO: incorporate extension behavior
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitCatch(CatchTree node, Void p) {
    // TODO: implement, with extension
    effStack.peek().mark();
    scan(node.getBlock(), p);
    effStack.peek().squashMark(node);
    // residual will be checked in block
    return p;
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
    // TODO extension checks
    // Set marker for scoping current
    effStack.peek().mark();
    scan(node.getVariable(), p);
    scan(node.getExpression(), p);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitContinue(ContinueTree node, Void p) {
    // TODO: implement, with extension. Non-trivial due to continue-to-label
    if (node.getLabel() == null) {
      if (extension.doesContinueCheck()) {
        effStack.peek().pushEffect(genericEffect.lift(extension.checkContinue(node)), node);
        effStack.peek().squashMark(node);
        checkResidual(node);
      }
    } else {
      throw new UnsupportedOperationException("");
    }
    return p;
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
    // Set mark for full expression
    effStack.peek().mark();

    scan(node.getStatement(), p);
    ControlEffectQuantale<X>.ControlEffect bodyEff = effStack.peek().latestEffect();
    scan(node.getCondition(), p);
    ControlEffectQuantale<X>.ControlEffect condEff = effStack.peek().latestEffect();

    // Here we DO NOT simply squash, because we must invoke iteration
    LinkedList<ControlEffectQuantale<X>.ControlEffect> pieces = effStack.peek().rewindToMark();
    assert (pieces.get(0) == bodyEff);
    assert (pieces.get(1) == condEff);

    ControlEffectQuantale<X>.ControlEffect repeff =
        genericEffect.iter(genericEffect.seq(bodyEff, condEff));
    if (repeff == null) {
      checker.reportError(node, "undefined.repetition.twopart", bodyEff, condEff);
      // Pretend we ran the loop and condition once each
      boolean success = effStack.peek().pushEffect(genericEffect.seq(bodyEff, condEff), node);
      assert success; // TODO fix
    } else {
      // Valid iteration
      ControlEffectQuantale<X>.ControlEffect eff = genericEffect.seq(condEff, repeff);
      boolean success = effStack.peek().pushEffect(eff, node);
      assert success; // TODO fix
    }
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
    // TODO implement: need to handle effects of implicit calls to the iterator methods
    throw new UnsupportedOperationException("foreach support does not exist yet");
  }

  @Override
  public Void visitForLoop(ForLoopTree node, Void p) {
    // TODO: extension
    // Set mark for full node
    effStack.peek().mark();

    // Scan the initializer statements (implicitly, in order)
    scan(node.getInitializer(), p);
    ControlEffectQuantale<X>.ControlEffect initEff = effStack.peek().latestEffect();
    scan(node.getCondition(), p);
    ControlEffectQuantale<X>.ControlEffect condEff = effStack.peek().latestEffect();
    scan(node.getStatement(), p);
    ControlEffectQuantale<X>.ControlEffect bodyEff = effStack.peek().latestEffect();
    // mark for updates, since there may be multiple
    effStack.peek().mark();
    scan(node.getUpdate(), p);
    ControlEffectQuantale<X>.ControlEffect updateEff = effStack.peek().squashMark(null);

    // If we're reached here, it's possible to run the initializers, cond, body, update in that
    // order
    // We care about iterating body-update-cond, though

    // Here we DO NOT simply squash, because we must invoke iteration
    LinkedList<ControlEffectQuantale<X>.ControlEffect> pieces = effStack.peek().rewindToMark();
    assert (pieces.get(0) == initEff);
    assert (pieces.get(1) == condEff);
    assert (pieces.get(2) == bodyEff);
    assert (pieces.get(3) == updateEff);

    ControlEffectQuantale<X>.ControlEffect repeff =
        genericEffect.iter(genericEffect.seq(genericEffect.seq(bodyEff, updateEff), condEff));
    if (repeff == null) {
      checker.reportError(node, "undefined.repetition.threepart", bodyEff, updateEff, condEff);
      // Pretend we ran the loop exactly once: init, condition, loop, update, and condition again
      effStack
          .peek()
          .pushEffect(genericEffect.seq(condEff, genericEffect.seq(bodyEff, condEff)), node);
    } else {
      // Valid iteration
      ControlEffectQuantale<X>.ControlEffect eff =
          genericEffect.seq(genericEffect.seq(initEff, condEff), repeff);
      effStack.peek().pushEffect(eff, node);
    }
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitIf(IfTree node, Void p) {
    return checkConditional(
        node, node.getCondition(), node.getThenStatement(), node.getElseStatement(), p);
  }

  protected Void checkConditional(
      Tree node, ExpressionTree condTree, Tree thenTree, Tree elseTree, Void p) {

    int contextSize = effStack.peek().size();

    // One mark for the whole node, nested mark for each branch.
    effStack.peek().mark();
    scan(condTree, p);
    boolean condError = errorOnCurrentPath;
    effStack.peek().mark();
    scan(thenTree, p);
    boolean thenError = errorOnCurrentPath;
    LinkedList<ControlEffectQuantale<X>.ControlEffect> thenEffs = effStack.peek().rewindToMark();
    assert (thenEffs.size() == 1);
    ControlEffectQuantale<X>.ControlEffect thenEff = thenEffs.get(0);
    ControlEffectQuantale<X>.ControlEffect elseEff = genericEffect.unit();
    // If there's no else, there's no else error
    boolean elseError = false;
    // If there was an error on the then-path, there may still be no issue on the else path
    errorOnCurrentPath = condError;
    if (elseTree != null) {
      effStack.peek().mark();
      scan(elseTree, p);
      elseError = errorOnCurrentPath;
      LinkedList<ControlEffectQuantale<X>.ControlEffect> elseEffs = effStack.peek().rewindToMark();
      assert (elseEffs.size() == 1);
      elseEff = elseEffs.get(0);
    }
    // stack still has the condition effect on it, but no branch effects
    effStack.peek().debugDump("@@@@@@", true);
    LinkedList<ControlEffectQuantale<X>.ControlEffect> condEffs = effStack.peek().rewindToMark();
    effStack.peek().debugDump("&&&&&&", true);
    System.err.println("thenEff == " + thenEff);
    System.err.println("elseEff == " + elseEff);
    assert (condEffs.size() == 1);
    ControlEffectQuantale<X>.ControlEffect condEff = condEffs.get(0);

    errorOnCurrentPath = condError || (thenError && elseError);

    if (errorOnCurrentPath) {
      // push a dummy effect
      // TODO: Really there should be a global approach to short-circuiting the visitor in this case
      // unless we can backtrack to an alternative worth exploring
      effStack.peek().pushEffect(genericEffect.unit(), node);
    } else if (thenError) {
      effStack.peek().pushEffect(genericEffect.seq(condEff, elseEff), node);
    } else if (elseError) {
      effStack.peek().pushEffect(genericEffect.seq(condEff, thenEff), node);
    } else {
      // Both branches possible and error-free: the common case
      ControlEffectQuantale<X>.ControlEffect lub = genericEffect.LUB(thenEff, elseEff);
      if (lub == null) {
        if (elseTree == null) {
          checker.reportError(node, "undefined.join.unaryif", thenEff, elseEff);
        } else {
          checker.reportError(node, "undefined.join", thenEff, elseEff);
        }
        errorOnCurrentPath = true;
        // push a dummy effect to make sure the stack size expectations are met.
        effStack.peek().pushEffect(genericEffect.unit(), node);
      } else {
        // This seq will always succeed (with valid EQs) since the seqs worked per-branch, and we
        // have
        // distributivity
        effStack.peek().pushEffect(genericEffect.seq(condEff, lub), node);

        // TODO: Figure out when we do/don't want multiple errors issued. Clearly want multiple for
        // cases like traditional LUB systems, but sometimes may want to stop early for truly
        // sequential EQs
        checkResidual(node);
      }
    }
    assert (effStack.peek().size() == 1 + contextSize);
    return p;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
    // TODO extension checks
    return checkConditional(
        node, node.getCondition(), node.getTrueExpression(), node.getFalseExpression(), p);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree node, Void p) {
    effStack.peek().mark();
    scan(node.getExpression(), p);
    if (extension.doesInstanceOfCheck()) {
      effStack.peek().pushEffect(genericEffect.lift(extension.checkInstanceOf(node)), node);
    }
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitIntersectionType(IntersectionTypeTree node, Void p) {
    if (extension.doesIntersectionTypeCheck()) {
      effStack.peek().pushEffect(genericEffect.lift(extension.checkIntersectionType(node)), node);
      checkResidual(node);
    }
    return p;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree node, Void p) {
    // TODO extension
    effStack.peek().mark();
    scan(node.getStatement(), p);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitLiteral(LiteralTree node, Void p) {
    // TODO extension
    // This has effect unit, shouldn't cause an error unless the extension is in use
    effStack.peek().pushEffect(genericEffect.unit(), node);
    // No need to check anything, we just pushed unit
    return p;
  }

  @Override
  public Void visitNewArray(NewArrayTree node, Void p) {
    // TODO extension
    // Note: We don't iterate even if there is a single initializer for all array cells, because the
    // expression is evaluated only once, and the value is duplicated
    effStack.peek().mark();
    for (ExpressionTree init : node.getInitializers()) {
      scan(init, p);
    }
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree node, Void p) {
    // TODO extension
    return p;
  }

  @Override
  public Void visitReturn(ReturnTree node, Void p) {
    // TODO extension
    // TODO: Need to handle early returns!!! This currently only handles return from tail position.
    // Exceptions I think are reasonable to punt on for now, but early returns are not
    // TODO: Perhaps I can just keep a stack of "early return effects", which I can handle by just
    // adding the effect so far to that stack? Oh, actually, don't even need that. There's no
    // residual check here, just a LE check!
    effStack.peek().mark();
    scan(node.getExpression(), p);
    effStack.peek().squashMark(node);
    // TODO: is looking at the top of the stack faster than getMethodCallerEffect ?
    if (!genericEffect.LE(effStack.peek().currentPathEffect(), getMethodCallerEffect())) {
      checker.reportError(
          node, "invalid.return", effStack.peek().currentPathEffect(), getMethodCallerEffect());
    }
    // TODO: Real question is what state I leave the stack in when returning. This will be hit
    // always at the end of a sequence of statements, but sometimes those will be nested inside a
    // conditional or loop.... and want the callers to know not to consider this path --- poison
    // value that can be inspected e.g. in conditional cases, which already save&restore?
    // TODO: Maybe the "set of behaviors" collection is the right way to handle exceptions, as long
    // as I track which exception leads to what... but then I need to handle methods that return
    // effects.... so I need a meta-annotation @ThrowsEffect(Class<?>, X)!
    effStack.peek().markImpossible(node);
    return p;
  }

  @Override
  public Void visitSwitch(SwitchTree node, Void p) {
    // TODO extension
    effStack.peek().mark();
    scan(node.getExpression(), p);
    // This is tricky: *assuming no fall-through*, we'll execute some number of case expressions,
    // followed by some (one) body, and possibly a default case.
    // TODO: Coordinate with CaseTree handling: visitCase should leave *two* elements on the stack,
    // so that this method can rewind and pick up separate effects for each expression and case
    // body, and stick them together. Actually, given that, it wouldn't be too much more work to
    // handle fall-through if I can just determine whether or not each case falls through. Maybe the
    // CFG component has some existing stuff I can use for that.
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public Void visitSynchronized(SynchronizedTree node, Void p) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public Void visitThrow(ThrowTree node, Void p) {
    effStack.peek().mark();
    scan(node.getExpression(), p);
    // TODO: HIPRI: This fails when the exception type is only in the target project, not in the
    // compiler's classpath. getClassFromType will return Object.class.
    TypeMirror m = TreeUtils.typeOf(node.getExpression());
    assert TypesUtils.isClassType(m);
    ClassType exctype = (ClassType) m;
    effStack
        .peek()
        .pushEffect(genericEffect.raise(exctype, getEnclosingThrowScopeTree(exctype), node), node);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitTry(TryTree node, Void p) {
    // TODO extension
    // throw new UnsupportedOperationException("not yet implemented");

    // Visit the body
    // take the body effect, and remove all nonlocal effects targeting this try block
    // For each catch block:
    //   Select the prefixes for exceptions subtypes of that caught exception, joining them (could
    // fail and lead to error)
    //   push that result as fake on the residual queue
    //   visit the catch block
    //   join the underlying into the main block's underlying effect (could fail)
    // Visit finally block
    //   Append its effect to every unresolved control effect (uncaught exceptions + breaks!)
    //   append to the tail of resolved exceptions as well
    // TODO: figure out how to fix residual checking to deal with finally blocks appending on the
    // right!!!
    // TODO: For that matter, figure out how to deal with fixing the residual checking to allow for
    // the underlying effect of a method to be formed in part by catching a thrown exception

    System.err.println("Visiting TRY: " + node);
    effStack.peek().mark();

    // Within a try block, the residual target must be extended to include locally-handled
    // exceptions
    // (We handle every exception type with a local catch block the same, but note that if a
    // particular exception is both locally handled and permitted to escape, and instances in the
    // try block could for example be rethrown wrapped in a different effect for escape, so handling
    // everything with a local catch the same is really the right call.)
    // Intuitively, the effect of an exception-to-catch path is the prefix of the corresponding
    // non-local effect from the body sequenced with the effect of the catch.
    // In general if there is a catch for exception E:
    // - The catch might return normally, in which case we care about residuals of
    // <prefix-to-E-throw> \ <context-residual-target-underlying>
    // - The catch might throw another exception F (either as a wrapped exception, or by abuse of
    // terminology the catch for E might simply throw F without passing E along), in which case we
    // care about residuals of <prefix-to-E-throw> \ <prefix-to-F-in-context-residual-target>
    // Therefore, the right contextual effect to register here is:
    //     current residual target + possible throw of any locally caught exception with prefix
    // copied from any throw prefix in the current target or the underlying target
    // This would give the most eager report, but may also be confusing for many users and would
    // probably have high runtime costs even for modest exception-handling code: for a contextual
    // target of size N behaviors and a try-catch with C catch blocks, this generates an effect of
    // size C*(N-1)+N (adjusting for duplicating only throws, not underlying, and keeping existing
    // throws).  So even a single catch would double the size of the effect.
    // In particular, rethrows are relatively rare, so in practice a prefix for E that *happens* to
    // residualize with a contextual prefix allowed for G would delay reporting until we visit the
    // catch block for E (with the current context) and resolve that E is not re-thrown as some
    // unrelated G.
    // That said, the scenario above is somewhat contrived, and itself probably uncommon.
    // Nonetheless, we compromise slightly on early reporting in order to mitigat the computational
    // concerns and error-reporting subtleties. We essentially mark the locally-caught exceptions as
    // "any-prefix-goes", and then check for real in conjunction with the catch block visit.
    Set<ClassType> caught = new HashSet<>();
    for (CatchTree cblk : node.getCatches()) {
      caught.add((ClassType) TreeUtils.typeOf(cblk.getParameter()));
    }
    residualTargets.addFirst(
        residualTargets.peek().withUnbounded(genericEffect.underlyingUnit(), caught, node));
    System.err.println(
        "TRY updated context for residuals to include " + caught + ": " + residualTargets.peek());
    effStack.peek().mark();
    scan(node.getBlock(), p);
    List<ControlEffectQuantale<X>.ControlEffect> bodyEffs = effStack.peek().rewindToMark();
    assert (bodyEffs.size() == 1);
    ControlEffectQuantale<X>.ControlEffect bodyEff = bodyEffs.get(0);
    System.err.println("--> body effect = " + bodyEff);
    residualTargets.removeFirst(); // Catch blocks are not handled by other peer catch blocks

    Set<Pair<ControlEffectQuantale<X>.ControlEffect, CatchTree>> catchpaths = new HashSet<>();

    Collection<Pair<ClassType, NonlocalEffect<X>>> unhandled = bodyEff.excs;
    for (CatchTree cblk : node.getCatches()) {
      // Each catch block runs after the prefixes of that throw
      Map<Boolean, List<Pair<ClassType, NonlocalEffect<X>>>> m =
          unhandled.stream()
              .collect(
                  Collectors.partitioningBy(
                      kv ->
                          atypeFactory
                              .getProcessingEnv()
                              .getTypeUtils()
                              .isSubtype(kv.first, TreeUtils.typeOf(cblk.getParameter()))));
      List<Pair<ClassType, NonlocalEffect<X>>> resolvedpaths = m.get(true);
      unhandled = m.get(false);
      assert (resolvedpaths.size() > 0) 
        : "No resolved paths for" + cblk.toString() + "\nMap is "+m+"\nUnhandled was originally "+unhandled;
      NonlocalEffect<X> lastHandled = null;
      X exclub = null;
      for (Pair<ClassType, NonlocalEffect<X>> eff : resolvedpaths) {
        assert atypeFactory
            .getProcessingEnv()
            .getTypeUtils()
            .isSubtype(eff.first, TreeUtils.typeOf(cblk.getParameter()));
        if (exclub == null) {
          // first entry
          exclub = eff.second.effect;
          lastHandled = eff.second;
        } else {
          // subsequent entries
          X tmp = xchecker.getEffectLattice().LUB(exclub, eff.second.effect);
          if (tmp == null) {
            throw new UnsupportedOperationException(
                "Implement good error messages for bad exc lubs; no lub of "
                    + lastHandled
                    + " and "
                    + eff);
          } else {
            exclub = tmp;
            lastHandled = eff.second;
          }
        }
      }
      effStack.peek().mark();
      // all LUB'ed paths are valid after the current prefix
      effStack.peek().mark();
      effStack.peek().pushEffect(genericEffect.lift(exclub), node.getBlock());
      scan(cblk, p);
      effStack.peek().squashMark(cblk);
      List<ControlEffectQuantale<X>.ControlEffect> throwcatchEffs = effStack.peek().rewindToMark();
      assert (throwcatchEffs.size() == 1);
      ControlEffectQuantale<X>.ControlEffect throwcatchEff = throwcatchEffs.get(0);
      catchpaths.add(Pair.of(throwcatchEff, cblk));
      // no need to check residual here, it was checked in the inner-most part of the catch block.
    }

    ControlEffectQuantale<X>.ControlEffect lub =
        bodyEff.filtering(caught); // TODO: WITH HANDLED EXCEPTIONS REMOVED
    for (Pair<ControlEffectQuantale<X>.ControlEffect, CatchTree> tcpath : catchpaths) {
      lub = genericEffect.LUB(lub, tcpath.first);
      if (lub == null) {
        throw new UnsupportedOperationException(
            "add nice errors: couldn't lub main path of try-catch with catch node path "
                + tcpath.second);
      }
    }


    // TODO: Okay, can't just append finally block effect to all entries in the exception map,
    // because there might be some in there from another branch of execution (e.g., the then branch
    // of a conditional, where this try is in the else block). The solution is to properly implement
    // C(X).
    if (node.getFinallyBlock() == null) {
      effStack.peek().pushEffect(lub, node);
      effStack.peek().squashMark(node);
    } else {
      effStack.peek().mark(); // extra mark so we can rewind everything
      // Record prior try-catch body for the finally block's residual checking to compare on the base effect
      effStack.peek().pushEffect(lub, node);
      effStack.peek().mark();
      scan(node.getFinallyBlock(), p);
      //effStack.peek().squashMark(node.getFinallyBlock());
      List<ControlEffectQuantale<X>.ControlEffect> finallyEffects = effStack.peek().rewindToMark();
      assert (finallyEffects.size() == 1);
      ControlEffectQuantale<X>.ControlEffect finallyEffect = finallyEffects.get(0);
      if (finallyEffect.hasControlBehaviors()) {
        throw new UnsupportedOperationException("Finally blocks that throw are not yet supported");
      }

      // TODO: combine the mark so far for this node with .appendFinallyBasic(finallyEffect.base)
      // If it's null, report an error (lots of room for improving the error message quality)
      // If not, use the result as the effect of this whole try-finally.
      ControlEffectQuantale<X>.ControlEffect overall = lub.appendFinallyBasic(finallyEffect.base);
      if (overall == null) {
        // TODO: Report every composition that failed, with detailed exception and path info
        checker.reportError(node.getFinallyBlock(), "undefined.finally.basic");
        errorOnCurrentPath = true;
      }
      effStack.peek().squashMark(node);
    }
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitTypeCast(TypeCastTree node, Void p) {
    effStack.peek().mark();
    scan(node.getExpression(), p);
    effStack.peek().pushEffect(genericEffect.lift(extension.checkTypeCast(node)), node);
    effStack.peek().squashMark(node);
    checkResidual(node);
    String warning = extension.reportWarning(node);
    if (warning != null) {
      checker.reportWarning(node, warning);
    }
    return p;
  }

  @Override
  public Void visitUnary(UnaryTree node, Void p) {
    // TODO extension
    effStack.peek().mark();
    scan(node.getExpression(), p);
    effStack.peek().squashMark(node);
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitUnionType(UnionTypeTree node, Void p) {
    if (extension.doesUnionTypeCheck()) {
      effStack.peek().pushEffect(genericEffect.lift(extension.checkUnionType(node)), node);
      checkResidual(node);
    }
    return p;
  }

  @Override
  public Void visitBlock(BlockTree node, Void p) {
    effStack.peek().mark();
    super.visitBlock(node, p);
    if (effStack.peek().currentlyImpossible()) {
      effStack.peek().rewindToMark();
      effStack.peek().markImpossible(node);
    } else {
      effStack.peek().squashMark(node);
    }
    return p;
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree node, Void p) {
    // Set mark for full expression
    effStack.peek().mark();
    scan(node.getCondition(), p);
    ControlEffectQuantale<X>.ControlEffect condEff = effStack.peek().latestEffect();
    scan(node.getStatement(), p);
    ControlEffectQuantale<X>.ControlEffect bodyEff = effStack.peek().latestEffect();

    // Here we DO NOT simply squash, because we must invoke iteration
    LinkedList<ControlEffectQuantale<X>.ControlEffect> pieces = effStack.peek().rewindToMark();
    assert (pieces.get(0) == condEff);
    assert (pieces.get(1) == bodyEff);

    ControlEffectQuantale<X>.ControlEffect repeff =
        genericEffect.iter(genericEffect.seq(bodyEff, condEff));
    if (repeff == null) {
      checker.reportError(node, "undefined.repetition.twopart", bodyEff, condEff);
      // Pretend we ran the condition, loop, and condition again
      effStack
          .peek()
          .pushEffect(genericEffect.seq(condEff, genericEffect.seq(bodyEff, condEff)), node);
    } else {
      // Valid iteration
      ControlEffectQuantale<X>.ControlEffect eff = genericEffect.seq(condEff, repeff);
      effStack.peek().pushEffect(eff, node);
    }
    checkResidual(node);
    return p;
  }

  @Override
  public Void visitWildcard(WildcardTree node, Void p) {
    if (extension.doesWildcardCheck()) {
      effStack.peek().pushEffect(genericEffect.lift(extension.checkWildcard(node)), node);
      checkResidual(node);
    }
    return p;
  }
}
