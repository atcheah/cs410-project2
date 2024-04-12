package utils;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;

import java.util.*;

/**
 * Util class intended to process certain nodes and extract information into desired structures.
 */
public class ParserUtil {

    /**
     * Extracts information from parameter names and inputted values into given Evaluator.
     * @param param
     * @param values
     * @param eval
     * @return - names of the defined variables.
     */
    public static Set<String> processParameters(NodeList<Parameter> param, NodeList<Expression> values,
                                                ExprEvaluator eval) {
        assert param.size() == values.size();
        assert eval != null;

        Set<String> definedParams = new HashSet<>();
        for (int i = 0; i < param.size(); i++) {
            Parameter p = param.get(i);
            Expression e = values.get(i);
            String assignmentStr = p.getName().asString() + "=" + e.toString();
            IExpr res = eval.eval(assignmentStr);
            eval.defineVariable(p.getNameAsString(), res);
            definedParams.add(p.getNameAsString());
        }
        return  definedParams;
    }

    /**
     * Helper that parses a parameter list into string of form: "(int i, boolean b)".
     * @param param
     * @return string of form "(int i, boolean b)"
     */
    public static String parseParameterListToString(NodeList<Parameter> param) {
        // To string returns form of
        String res = param.toString();
        res = "(" + res.substring(1, res.length() - 1) + ")";
        return res;
    }
}
