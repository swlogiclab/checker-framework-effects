package checkers.regex;

import java.util.List;

import javax.lang.model.type.TypeKind;

import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type.ArrayType;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.util.TreeUtils;

/**
 * Adds {@link Regex} to the type of tree, in the following cases:
 *
 * <ol>
 *
 * <li value="1">a {@code String} or (@code char} literal that is a valid
 * regular expression</li>
 *
 * <li value="2">concatenation tree of two valid regular expression values
 * (either {@code String} or {@code char}.)</li>
 * 
 * <li value="3">initialization of a char array that when converted to a String
 * is a valid regular expression.</li>
 *
 * </ol>
 *
 * Also, adds {@link PolyRegex} to the type of String/char concatenation of
 * a Regex and a PolyRegex or two PolyRegexs.
 */
public class RegexAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<RegexChecker> {

    public RegexAnnotatedTypeFactory(RegexChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public TreeAnnotator createTreeAnnotator(RegexChecker checker) {
        return new RegexTreeAnnotator(checker);
    }

    private class RegexTreeAnnotator extends TreeAnnotator {

        public RegexTreeAnnotator(BaseTypeChecker checker) {
            super(checker, RegexAnnotatedTypeFactory.this);
        }

        /**
         * Case 1: valid regular expression String or char literal
         */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()) {
                boolean regexString = tree.getKind() == Tree.Kind.STRING_LITERAL
                                      && RegexUtil.isRegex((String) tree.getValue());
                boolean regexChar = tree.getKind() == Tree.Kind.CHAR_LITERAL
                                    && RegexUtil.isRegex((Character) tree.getValue());
                if (regexString || regexChar) {
                    type.addAnnotation(Regex.class);
                }
            }
            return super.visitLiteral(tree, type);
        }

        /**
         * Case 2: concatenation of Regex or PolyRegex String/char literals
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && TreeUtils.isStringConcatenation(tree)) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(tree.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(tree.getRightOperand());
                
                boolean lExprRE = lExpr.hasAnnotation(Regex.class);
                boolean rExprRE = rExpr.hasAnnotation(Regex.class);
                boolean lExprPoly = lExpr.hasAnnotation(PolyRegex.class);
                boolean rExprPoly = rExpr.hasAnnotation(PolyRegex.class);
                
                if (lExprRE && rExprRE)
                    type.addAnnotation(Regex.class);
                else if (lExprPoly && rExprPoly
                        || lExprPoly && rExprRE
                        || lExprRE && rExprPoly)
                    type.addAnnotation(PolyRegex.class);
            }
            return super.visitBinary(tree, type);
        }
        
        /**
         * Case 3: a char array that as a String is a valid regular expression.
         */
        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
            boolean isCharArray = ((ArrayType) type.getUnderlyingType())
                    .getComponentType().getKind() == TypeKind.CHAR;
            if (isCharArray && tree.getInitializers() != null) {
                List<? extends ExpressionTree> initializers = tree.getInitializers();
                String charArray = "";
                boolean allLiterals = true;
                for (int i = 0; allLiterals && i < initializers.size(); i++) {
                    ExpressionTree e = initializers.get(i);
                    if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                        charArray += ((LiteralTree) e).getValue();
                    } else if (getAnnotatedType(e).hasAnnotation(Regex.class)) {
                        // if there's an @Regex char in the array then substitute
                        // it with a .
                        charArray += '.';
                    } else {
                        allLiterals = false;
                    }
                }
                if (allLiterals && RegexUtil.isRegex(charArray)) {
                    type.addAnnotation(Regex.class);
                }
            }
            return super.visitNewArray(tree, type);
        }
    }
}
