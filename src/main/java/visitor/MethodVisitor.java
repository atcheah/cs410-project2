package visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import graph.*;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import utils.ParserUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodVisitor extends VoidVisitorAdapter<StateWrapper> {

    private final Set<MethodDeclaration> methodDeclarations;
    private Stack<List<GraphNodeTuple>> statementStack;
    private Stack<ExprEvaluator> variableStack;
    private Stack<Set<String>> variableNameStack; // Used to keep track of the variables available, so we can clone the variable state
    private Map<String, GraphNode> cyclicMethodCallMap;

    private final String symjaVariablePrefix = "_"; // TODO: There are reserved keywords in Symja (like sum, length, underscores (USE camelCase) so do not use in a variable name at all)

    // Space in between words so that it prevents overlapping with any user
    // defined variable names.
    private final String continueCalledStringName = "continue called";

    public MethodVisitor(Set<MethodDeclaration> methodDeclarations) {
        this.methodDeclarations = methodDeclarations;
        this.statementStack = new Stack<>();
        this.variableStack = new Stack<>();
        this.variableNameStack = new Stack<>();
        this.cyclicMethodCallMap = new HashMap<>();
    }

    @Override
    public void visit(MethodDeclaration md, StateWrapper arg) {
        String paramString = ParserUtil.parseParameterListToString(md.getParameters());
        GraphNode graphNode = new GraphNode(BlockType.METHOD_DECL, md.getName().asString() + paramString);

        // Top-level declaration, reset our stack
        if (md.getParentNode().isPresent() && md.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
            this.statementStack.clear();
            this.variableStack.clear();
            this.variableNameStack.clear();
            arg.rootNodes.add(graphNode);
            this.cyclicMethodCallMap.clear();
            this.cyclicMethodCallMap.put(md.getName().asString(), graphNode);
        }

        boolean areAllIncomingPathsDead = areAllIncomingPathsDead();
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(graphNode, areAllIncomingPathsDead)));
        this.variableStack.add(new ExprEvaluator());
        this.variableNameStack.add(new HashSet<>());
        super.visit(md, arg);
        this.variableNameStack.pop();
        this.variableStack.pop();
        this.statementStack.pop();
    }

    @Override
    public void visit(MethodCallExpr mce, StateWrapper arg) {
        // Connect this to the previous statement
        List<GraphNodeTuple> parents = this.statementStack.peek();
        GraphNode child = new GraphNode(BlockType.METHOD_CALL, mce.toString());

        for(GraphNodeTuple parent: parents) {
            GraphEdge graphEdge = new GraphEdge(child, parent.textFromParentToChild, parent.pathToChildIsDead);
            parent.node.addEdge(graphEdge);
        }

        boolean areAllIncomingPathsDead = areAllIncomingPathsDead();

        try {
            // Resolve the method being called - will only go into the method if it is a
            // top-level declaration in the given file
            ResolvedMethodDeclaration resolvedMethod = mce.resolve();
            Optional<Node> methodDeclarationNode = resolvedMethod.toAst();
            if (methodDeclarationNode.isPresent() && methodDeclarationNode.get() instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) methodDeclarationNode.get();
                if (this.methodDeclarations.contains(md)) {
                    if (!this.cyclicMethodCallMap.containsKey(md.getName().asString())) {
                        // Do not try to go into cyclic calls
                        // Create new symbol table/evaluator. TODO: need to modify this if we want to support class level
                        ExprEvaluator util = new ExprEvaluator();
                        this.cyclicMethodCallMap.put(md.getName().asString(), child);

                        Set<String> declParams = ParserUtil.processParameters(md.getParameters(), mce.getArguments(), util);
                        this.statementStack.add(Arrays.asList(new GraphNodeTuple(child, areAllIncomingPathsDead)));
                        this.variableStack.add(util);
                        this.variableNameStack.add(declParams);
                        super.visit(md, arg);
                        this.cyclicMethodCallMap.remove(md.getName().asString());


                        // TODO: need to pop stack here?
                        this.statementStack.pop();
                        this.variableStack.pop();
                        this.variableNameStack.pop();
                        return;
                    }
                }
            }
        } catch (UnsolvedSymbolException e) {
            System.out.println("Method call could not be properly resolved: " + child.toString());
        }
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(child, areAllIncomingPathsDead)));
        super.visit(mce, arg);
    }

    @Override
    public void visit(IfStmt is, StateWrapper arg) {
        // Connect this statement to previous statement(s)
        List<GraphNodeTuple> parents = this.statementStack.peek();
        GraphNode conditional = new GraphNode(BlockType.IF, "if (" + is.getCondition().toString() + ")");
        for (GraphNodeTuple parent: parents) {
            GraphEdge parentConditional = new GraphEdge(conditional, parent.textFromParentToChild, parent.pathToChildIsDead);
            parent.node.addEdge(parentConditional);
        }

        // Keep a copy of the stack prior to visiting any branch conditions
        ExprEvaluator util = this.variableStack.peek();
        boolean isDeadBeforeConditionals = areAllIncomingPathsDead();
        ExprEvaluator evaluatorBeforeConditionals = deepcopyEvaluator();
        Set<String> variablesBeforeConditionals = deepcopyVariableNames();

        // A result of "False" means that the ELSE branch is taken
        boolean thenBranchIsDead = isDeadBeforeConditionals;
        IExpr conditionalResult = evaluateSymjaExpression(util, is.getCondition().toString());
        if (isDeadBeforeConditionals || Objects.equals(conditionalResult.toString(), "False")) {
            thenBranchIsDead = true;
        }

        // Add the conditional block to the stack so that deeper statements refer back to this
        List<GraphNodeTuple> thenCondition = Arrays.asList(new GraphNodeTuple(conditional, "then", thenBranchIsDead));
        this.statementStack.add(thenCondition);
        is.getThenStmt().accept(this, arg); // Visit the IF block

        // Copy the evaluator after evaluating the THEN branch
        ExprEvaluator evaluatorAfterThenBranch = deepcopyEvaluator();
        Set<String> variablesAfterThenBranch = deepcopyVariableNames();

        // Grab the last statement executed in the if block
        List<GraphNodeTuple> lastStatements = new ArrayList<>(statementStack.peek());

        // Reset the stack so the `else` statements that follow are nested under the `conditional` block
        while (this.statementStack.peek() != parents) {
            this.statementStack.pop();
        }

        // A result of "True" means that the THEN branch is taken
        boolean elseBranchIsDead = isDeadBeforeConditionals;
        if (isDeadBeforeConditionals || Objects.equals(conditionalResult.toString(), "True")) {
            elseBranchIsDead = true;
        }
        List<GraphNodeTuple> elseCondition = Arrays.asList(new GraphNodeTuple(conditional, "else", elseBranchIsDead));
        this.statementStack.add(elseCondition);

        // Replace the stack with the one prior to visiting any conditions
        this.variableStack.pop();
        this.variableNameStack.pop();
        this.variableStack.add(evaluatorBeforeConditionals);
        this.variableNameStack.add(variablesBeforeConditionals);

        Optional<Statement> elseStmt = is.getElseStmt();
        if (elseStmt.isPresent()) {
            elseStmt.get().accept(this, arg);
            lastStatements.addAll(this.statementStack.peek());
        } else {
            // If there is no ELSE block, then we need to connect the CONDITIONAL to the child statements
            lastStatements.add(new GraphNodeTuple(conditional, "else", isDeadBeforeConditionals));
        }

        // Copy the evaluator after evaluating the ELSE branch
        ExprEvaluator evaluatorAfterElseBranch = deepcopyEvaluator();
        Set<String> variablesAfterElseBranch = deepcopyVariableNames();


        // TODO:
        // If continue called = true in an eval branch (Either IF/ELSE) and we know that the branch is run
        // FOR SURE (eg. thenBranchDead && !elseBranchDead = we ran else branch for sure), if continue called is
        // true in else branch, all children after conditional = dead.
        IExpr ccInThen = evaluatorAfterThenBranch.getVariable(continueCalledStringName);
        IExpr ccInElse = evaluatorAfterElseBranch.getVariable(continueCalledStringName);
        boolean ccRanInThen =
                !thenBranchIsDead && elseBranchIsDead && ccInThen != null && ccInThen.toString().equals("True");
        boolean ccRanInElse =
                thenBranchIsDead && !elseBranchIsDead && ccInElse != null && ccInElse.toString().equals("True");
        if (ccRanInThen || ccRanInElse) {
            // Continue called in ran THEN branch.
            // Set all lastStatements children to be dead?? TODO: if in affected loop
            for (GraphNodeTuple tuple : lastStatements) {
                tuple.pathToChildIsDead = true;
            }
        }

        // Reconcile the stack of the variable evaluator
        ExprEvaluator reconciledEvaluator = reconcileEvaluator(variablesAfterElseBranch, evaluatorAfterElseBranch,
                variablesAfterThenBranch, evaluatorAfterThenBranch, elseBranchIsDead, thenBranchIsDead);


        // Reset state of 'continue called' TODO: maybe need to do this at loop level and not in IF.
        // reconciledEvaluator.defineVariable(continueCalledStringName, false);

        this.variableStack.pop();
        this.variableStack.add(reconciledEvaluator);

        lastStatements.removeIf(gnt -> gnt.node.blockType == BlockType.RETURN);
        // Now both the terminal statements of this conditional block will connect to the next node
        this.statementStack.add(lastStatements);
    }

    private IExpr evaluateSymjaExpression(ExprEvaluator util, String expression) {
        expression = replaceStringLength(expression);
        expression = replaceStringEquals(expression);
        expression = replaceModSymbolExpr(expression);
        expression = replaceArrayLength(util, expression);
        return util.eval(expression);
    }

    private String replaceStringLength(String expression) {
        /** Start Code Generated by [ChatGPT] on [April 4, 2024] */
        // Symja does have a way evaluate the length of a string; however, we need to change the syntax:
        //  some_string.length() -> StringLength(some_string)
        String regex = "(.*)\\.length\\(\\)";
        String replacement = "StringLength($1)";

        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(expression);

        if (match.find()) {
            String result = expression.replaceFirst(regex, replacement);
            return result;
        }

        return expression;
        /** End Code Generated by [ChatGPT] on [April 4, 2024] */
    }

    private String replaceStringEquals(String expression) {
        /** Start Code Generated by [ChatGPT] on [April 4, 2024] */
        // Symja does have a way evaluate the length of a string; however, we need to change the syntax:
        //  X.equals("TEST")
        String regex = "(.*?)\\.equals\\(\"(.*?)\"\\)";
        String replacement = "$1 == \"$2\"";

        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(expression);

        if (match.find()) {
            String result = expression.replaceFirst(regex, replacement);
            return result;
        }

        return expression;
        /** End Code Generated by [ChatGPT] on [April 4, 2024] */
    }

    private String replaceArrayLength(ExprEvaluator util, String expression) {
        /** Start Code Generated by [ChatGPT] on [April 4, 2024] */
        // Symja does have a way evaluate the length of an array; however, we need to change the syntax:
        //  some_array.size() -> Length(some_array)

        String regex = "(.*)\\.length";
        String replacement = "Length($1)";

        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(expression);

        if (match.find()) {
            String arrVariable = match.group(1);
            IExpr result = util.getVariable(arrVariable);
            if (result == null) {
                // For symbolic arrays, Symja always returns a length of 0 - we don't want this, so just return the variable name instead
                return arrVariable;
            }

            return expression.replaceFirst(regex, replacement);
        }

        return expression;
        /** End Code Generated by [ChatGPT] on [April 4, 2024] */
    }
    private String replaceModSymbolExpr(String expression) {
        expression = expression.trim();
        /** Start Code Generated by [ChatGPT] on [April 7, 2024] */
        String regex = "\\b(\\w+)\\s*%\\s*(\\w+)\\b";
        /** End Code Generated by [ChatGPT] on [April 7, 2024] */

        String replacement = "Mod($1, $2)";

        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(expression);

        if (match.find()) {
            String result = expression.replaceFirst(regex, replacement);
            return result;
        }

        return expression;
    }


    @Override
    public void visit(VariableDeclarationExpr vde, StateWrapper arg) {
        // TODO: Figure out a more elegant way to omit double processing these
        // Inside the for loop we declare `int i = 0`, without this guard we end up double printing
        if (!(vde.getParentNode().isPresent() && (vde.getParentNode().get() instanceof ForStmt || vde.getParentNode().get() instanceof ForEachStmt))) {
            // Link this node to parent(s)
            List<GraphNodeTuple> parents = this.statementStack.peek();
            GraphNode child = new GraphNode(BlockType.STMT, vde.toString());
            for (GraphNodeTuple parent: parents) {
                GraphEdge graphEdge = new GraphEdge(child, parent.textFromParentToChild, parent.pathToChildIsDead);
                parent.node.addEdge(graphEdge);
            }

            // Add this node on top of the stack for future statements
            boolean areAllIncomingPathsDead = areAllIncomingPathsDead();
            this.statementStack.add(Arrays.asList(new GraphNodeTuple(child, areAllIncomingPathsDead)));

            // Add this variable to our evaluator state
            String variable = vde.getVariables().get(0).getNameAsString();
            String value = vde.getVariables().get(0).getInitializer().get().toString();
            ExprEvaluator evaluator = this.variableStack.peek();
            IExpr result = evaluateSymjaExpression(evaluator, value);
            evaluator.defineVariable(variable, result);

            this.variableNameStack.peek().add(variable);
        }

        super.visit(vde, arg);
    }

    @Override
    public void visit(AssignExpr ae, StateWrapper arg) {
        // Link this node to parent(s)
        List<GraphNodeTuple> parents = this.statementStack.peek();
        GraphNode child = new GraphNode(BlockType.STMT, ae.toString());
        for (GraphNodeTuple parent: parents) {
            GraphEdge graphEdge = new GraphEdge(child, parent.textFromParentToChild, parent.pathToChildIsDead);
            parent.node.addEdge(graphEdge);
        }

        // Add this node on top of the stack for future statements
        boolean areAllIncomingPathsDead = areAllIncomingPathsDead();
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(child, areAllIncomingPathsDead)));

        // Modify this variable in our evaluator state
        String variable = ae.getTarget().toString();
        String value = ae.getValue().toString();
        ExprEvaluator evaluator = this.variableStack.peek();
        IExpr result = evaluateSymjaExpression(evaluator, value);
        evaluator.defineVariable(variable, result);

        this.variableNameStack.peek().add(variable);

        super.visit(ae, arg);
    }

    @Override
    public void visit(ForEachStmt fes, StateWrapper arg) {

        VariableDeclarationExpr initialization = (VariableDeclarationExpr) fes.getVariable();
        NameExpr iterable = (NameExpr) fes.getIterable();

        List<GraphNodeTuple> parents = this.statementStack.peek();
        GraphNode forLoopStmt = new GraphNode(BlockType.FOREACH, String.format("for(%s : %s)", initialization.toString(), iterable.toString()));
        for (GraphNodeTuple parent: parents) {
            GraphEdge parentForLoop = new GraphEdge(forLoopStmt, parent.textFromParentToChild, parent.pathToChildIsDead);
            parent.node.addEdge(parentForLoop);
        }

        boolean areAllIncomingPathsDead = areAllIncomingPathsDead();
        GraphNode iteratorStmt = new GraphNode(BlockType.STMT, "iterator()");
        GraphEdge forLoopIterator = new GraphEdge(iteratorStmt, "", areAllIncomingPathsDead);
        forLoopStmt.addEdge(forLoopIterator);

        GraphNode conditionalStmt = new GraphNode(BlockType.IF, "iterator.hasNext()");
        GraphEdge iteratorConditional = new GraphEdge(conditionalStmt, "", areAllIncomingPathsDead);
        iteratorStmt.addEdge(iteratorConditional);

        // Save a copy of the evaluator prior to all the loop statements
        ExprEvaluator evaluatorBeforeLoop = deepcopyEvaluator();
        Set<String> variablesBeforeLoop = deepcopyVariableNames();
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(conditionalStmt, "then", areAllIncomingPathsDead)));

        GraphNode nextStmt = new GraphNode(BlockType.STMT, "iterator.next()");
        arg.loopStates.push(new LoopState(conditionalStmt, nextStmt, false));
        super.visit(fes, arg);
        arg.loopStates.pop();

        // Reconcile evaluator after all the loop statements
        ExprEvaluator evaluatorAfterLoop = this.variableStack.peek();
        Set<String> variablesAfterLoop = this.variableNameStack.peek();
        ExprEvaluator reconciledEvaluator = reconcileEvaluator(variablesAfterLoop, evaluatorAfterLoop,
                variablesBeforeLoop, evaluatorBeforeLoop);

        this.variableStack.pop();
        this.variableStack.add(reconciledEvaluator);

        ResetContinueVariableAfterLoop(reconciledEvaluator);

        GraphNode lastStatementInForLoop = this.statementStack.peek().get(0).node;

        GraphEdge lastStatementNext = new GraphEdge(nextStmt, "", areAllIncomingPathsDead);
        lastStatementInForLoop.addEdge(lastStatementNext);

        // iterator.next() -> conditional will never be dead.
        GraphEdge nextHasNext = new GraphEdge(conditionalStmt, "", areAllIncomingPathsDead);
        nextStmt.addEdge(nextHasNext);

        // Connect future statements to the `!hasNext()` case
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(conditionalStmt, "else", areAllIncomingPathsDead)));
    }

    @Override
    public void visit(ForStmt fs, StateWrapper arg) {
        List<GraphNodeTuple> parents = this.statementStack.peek();

        // eg. int i = 0;
        VariableDeclarationExpr initialization = (VariableDeclarationExpr) fs.getInitialization().get(0);

        // eg. i < 8;
        BinaryExpr conditional = (BinaryExpr) fs.getCompare().get();

        // eg. i++;
        UnaryExpr iteratorUpdate = (UnaryExpr) fs.getUpdate().get(0);

        // Create node with example text: for(int i = 0; i < 8; i++)
        GraphNode forLoopStmt = new GraphNode(BlockType.FOR, String.format("for(%s; %s; %s)",
                initialization.toString(),
                conditional,
                iteratorUpdate.toString()));

        // Link this node to parent.
        for (GraphNodeTuple parent: parents) {
            GraphEdge parentEdge = new GraphEdge(forLoopStmt, parent.textFromParentToChild, parent.pathToChildIsDead);
            parent.node.addEdge(parentEdge);
        }

        // Node with conditional text. eg: i < 8
        GraphNode conditionalStmt = new GraphNode(BlockType.IF, conditional.toString());

        // Link: [for(...)] -> [i < 8]
        boolean areAllIncomingPathsDead = areAllIncomingPathsDead();
        forLoopStmt.addEdge(new GraphEdge(conditionalStmt, "", areAllIncomingPathsDead));

        // Save a copy of the evaluator prior to all the loop statements
        ExprEvaluator evaluatorBeforeLoop = deepcopyEvaluator();
        Set<String> variablesBeforeLoop = deepcopyVariableNames();


        // Set [i < 8] as parent node of first body node. Visit Body.
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(conditionalStmt, "then", areAllIncomingPathsDead)));

        // Node with update text. eg: i++
        GraphNode updateStmt = new GraphNode(BlockType.STMT, iteratorUpdate.toString());
        arg.loopStates.push(new LoopState(conditionalStmt, updateStmt,false));
        super.visit(fs, arg);
        arg.loopStates.pop();

        // Reconcile evaluator after all the loop statements
        ExprEvaluator evaluatorAfterLoop = this.variableStack.peek();
        Set<String> variablesAfterLoop = this.variableNameStack.peek();
        ExprEvaluator reconciledEvaluator = reconcileEvaluator(variablesAfterLoop, evaluatorAfterLoop,
                variablesBeforeLoop, evaluatorBeforeLoop);

        this.variableStack.pop();
        this.variableStack.add(reconciledEvaluator);

        ResetContinueVariableAfterLoop(reconciledEvaluator);


        // Last node statement from for loop body. eg: int x = i;
        GraphNode lastBodyNode = this.statementStack.peek().get(0).node;

        // Link [lastBodyNode] -> [i++]
        lastBodyNode.addEdge(new GraphEdge(updateStmt, "", areAllIncomingPathsDead));

        // Link [i++] -> [i < 8] (this is never dead)
        updateStmt.addEdge(new GraphEdge(conditionalStmt, "", areAllIncomingPathsDead));

        // Connect future statements to [!(i < 8)]
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(conditionalStmt, "else", areAllIncomingPathsDead)));

    }

    @Override
    public void visit(SwitchStmt ss, StateWrapper arg) {
        List<GraphNodeTuple> parents = this.statementStack.peek();
        GraphNode selector = new GraphNode(BlockType.SWITCH, "switch(" +ss.getSelector().toString() + ")");
        for (GraphNodeTuple parent: parents) {
            GraphEdge parentSelector = new GraphEdge(selector, "", parent.pathToChildIsDead);
            parent.node.addEdge(parentSelector);
        }

        ExprEvaluator evaluatorBeforeSwitch = deepcopyEvaluator();
        Set<String> variablesBeforeSwitch = deepcopyVariableNames();
        ExprEvaluator evaluatorAfterCase = deepcopyEvaluator();
        Set<String> variablesAfterCase = deepcopyVariableNames();

        boolean isDeadBefore = areAllIncomingPathsDead();
        List<GraphNodeTuple> selectorL = Arrays.asList(new GraphNodeTuple(selector,
                ss.getSelector().toString(), isDeadBefore));
        this.statementStack.add(selectorL);
        List<GraphNodeTuple> lastStatements = new ArrayList<>();
        List<GraphNodeTuple> parent = new ArrayList<>();
        for(SwitchEntry se: ss.getEntries()) {
            while (this.statementStack.peek() != selectorL) {
                this.statementStack.pop();
            }
            parent.add(new GraphNodeTuple(selector, ss.getSelector().toString(), isDeadBefore));
            this.statementStack.add(parent);
            se.accept(this, arg);
            parent.clear();
            evaluatorAfterCase = this.variableStack.pop();
            variablesAfterCase = this.variableNameStack.pop();
            for (GraphNodeTuple gnt : this.statementStack.pop()) {
                if (gnt.node.blockType == BlockType.BREAK) {
                    lastStatements.add(gnt);
                    this.variableStack.add(evaluatorBeforeSwitch);
                    this.variableNameStack.add(variablesBeforeSwitch);
                } else {
                    if (gnt.node.blockType != BlockType.RETURN) {
                        parent.add(gnt);
                        this.variableStack.add(evaluatorAfterCase);
                        this.variableNameStack.add(variablesAfterCase);
                    } else {
                        this.variableStack.add(evaluatorBeforeSwitch);
                        this.variableNameStack.add(variablesBeforeSwitch);
                    }
                }
            }
        }

        this.variableStack.pop();
        this.variableNameStack.pop();
        ExprEvaluator symbolic = new ExprEvaluator();
        for (String var : variablesBeforeSwitch) {
            symbolic.defineVariable(var);
        }
        this.variableStack.add(symbolic);
        this.variableNameStack.add(variablesBeforeSwitch);

        lastStatements.addAll(this.statementStack.peek());
        lastStatements.removeIf(gnt -> gnt.node.blockType == BlockType.RETURN);
        this.statementStack.add(lastStatements);
    }

    @Override
    public void visit(SwitchEntry se, StateWrapper arg) {
        int setLabel = 1 + se.getLabels().size();
        List<GraphNodeTuple> parents = this.statementStack.peek();
        ExprEvaluator util = this.variableStack.peek();
        IExpr result;
        if (setLabel > 1) {
            result = evaluateSymjaExpression(util, "(" + parents.get(parents.size() - 1).textFromParentToChild + ") == " + se.getLabels().get(0).toString());
        } else {
            result = evaluateSymjaExpression(util, "true");
        }
        boolean isDead = parents.get(0).pathToChildIsDead || Objects.equals(result.toString(), "False");
        for(Statement s: se.getStatements()) {
            s.accept(this, arg);
            if (setLabel > 1) {
                List<GraphEdge> edges = parents.get(0).node.edges;
                GraphEdge edge = edges.get(edges.size() - 1);
                edge.label = "case " + se.getLabels().get(0).toString();
                edge.isDead = isDead;
            } else if (setLabel == 1) {
                parents.get(0).node.edges.get(parents.get(0).node.edges.size() - 1).label = "default";
            }
            setLabel = 0;
        }
    }

    @Override
    public void visit(BreakStmt bs, StateWrapper arg) {
        GraphNode breakNode = new GraphNode(BlockType.BREAK, "break");
        for (GraphNodeTuple gnt : this.statementStack.peek()) {
            gnt.node.addEdge(new GraphEdge(breakNode, "", gnt.pathToChildIsDead));
        }
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(breakNode, areAllIncomingPathsDead())));
    }

    @Override
    public void visit(ReturnStmt rs, StateWrapper arg) {
        List<GraphNodeTuple> parents = this.statementStack.peek();
        GraphNode returnNode = new GraphNode(BlockType.RETURN, rs.toString());
        for (GraphNodeTuple gnt: parents) {
            GraphEdge edge = new GraphEdge(returnNode, "", gnt.pathToChildIsDead);
            gnt.node.addEdge(edge);
        }
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(returnNode, areAllIncomingPathsDead())));
    }

    @Override
    public void visit(WhileStmt ws, StateWrapper arg) {
        List<GraphNodeTuple> parents = this.statementStack.peek();

        // eg. oddNumber % 2 != 0
        Expression conditional = ws.getCondition();

        // Construct node: [while(oddNumber % 2 != 0)]
        GraphNode whileStmt = new GraphNode(BlockType.WHILE, String.format("while(%s)",
                conditional));

        // Link this node to parents.
        for (GraphNodeTuple parent: parents) {
            GraphEdge parentEdge = new GraphEdge(whileStmt, parent.textFromParentToChild, parent.pathToChildIsDead);
            parent.node.addEdge(parentEdge);
        }

        // Construct node: [oddNumber % 2 != 0]
        GraphNode conditionalStmt = new GraphNode(BlockType.IF, conditional.toString());

        // Link [while(...)] -> [oddNumber % 2 != 0]
        boolean areAllIncomingPathsDead = areAllIncomingPathsDead();
        whileStmt.addEdge(new GraphEdge(conditionalStmt, "", areAllIncomingPathsDead));

        // Save a copy of the evaluator prior to all the loop statements
        ExprEvaluator evaluatorBeforeLoop = deepcopyEvaluator();
        Set<String> variablesBeforeLoop = deepcopyVariableNames();

        // Set [oddNumber % 2 != 0] as parent node of first body node. Visit Body.
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(conditionalStmt, "then", areAllIncomingPathsDead)));
        arg.loopStates.push(new LoopState(conditionalStmt, null, false)); // While loops don't have an iterator update.
        super.visit(ws, arg);
        arg.loopStates.pop();

        // Reconcile evaluator after all the loop statements
        ExprEvaluator evaluatorAfterLoop = this.variableStack.peek();
        Set<String> variablesAfterLoop = this.variableNameStack.peek();
        ExprEvaluator reconciledEvaluator = reconcileEvaluator(variablesAfterLoop, evaluatorAfterLoop,
                variablesBeforeLoop, evaluatorBeforeLoop);

        this.variableStack.pop();
        this.variableStack.add(reconciledEvaluator);
        ResetContinueVariableAfterLoop(reconciledEvaluator);

        // Last node statement from for loop body. eg: int x = i;
        GraphNode lastBodyNode = this.statementStack.peek().get(0).node;

        // Link [lastBodyNode] -> [oddNumber % 2 != 0]
        lastBodyNode.addEdge(new GraphEdge(conditionalStmt, "", areAllIncomingPathsDead));

        // Connect future statements to [!(oddNumber % 2 != 0)]
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(conditionalStmt, "else", areAllIncomingPathsDead)));
    }

    @Override
    public void visit(ContinueStmt cs, StateWrapper arg) {
        // We want to point this node to the conditional of the WHILE/FOR loop
        List<GraphNodeTuple> parents = this.statementStack.peek();
        GraphNode continueNode = new GraphNode(BlockType.STMT, "continue");
        // Link parents to this node.
        for (GraphNodeTuple parent : parents) {
            GraphEdge parentEdge = new GraphEdge(continueNode, parent.textFromParentToChild, parent.pathToChildIsDead);
            parent.node.addEdge(parentEdge);
        }

        // Set Eval variable of 'continue called' = true.
        ExprEvaluator evaluator = this.variableStack.peek();
        IExpr result = evaluateSymjaExpression(evaluator, continueCalledStringName + " = true");
        evaluator.defineVariable(continueCalledStringName, result);
        this.variableNameStack.peek().add(continueCalledStringName);

        arg.loopStates.peek().containsContinue = true;

        // TODO: Check if continue is called within a loop! (Or state this as an invariant that users must follow.)

        // All other nodes after continue are unreachable, aka 'Dead'
        this.statementStack.add(Arrays.asList(new GraphNodeTuple(continueNode, true)));

        // Link this node to its closest loop updater or loop conditional (if updater == null).
        if (arg.loopStates.peek().loopUpdater != null) {
            continueNode.addEdge(new GraphEdge(arg.loopStates.peek().loopUpdater, "continue", false));
        } else {
            continueNode.addEdge(new GraphEdge(arg.loopStates.peek().loopConditional, "continue", false));
        }
    }
    
    private ExprEvaluator reconcileEvaluator(Set<String> variablesAfterElseBranch,
            ExprEvaluator evaluatorAfterElseBranch, Set<String> variablesAfterThenBranch,
            ExprEvaluator evaluatorAfterThenBranch, boolean elseBranchDead, boolean thenBranchDead) {

        if (elseBranchDead == thenBranchDead) {
            // Either they are both dead or both alive - then we reconcile
            return reconcileEvaluator(variablesAfterElseBranch, evaluatorAfterElseBranch, variablesAfterThenBranch,
                    evaluatorAfterThenBranch);
        } else if (elseBranchDead) {
            // Take the evaluator from the THEN branch (ELSE is not taken)
            return evaluatorAfterThenBranch;
        } else {
            // Take the evaluator from the ELSE branch (THEN is not taken)
            return evaluatorAfterElseBranch;
        }
    }

    private ExprEvaluator reconcileEvaluator(Set<String> variablesA, ExprEvaluator evaluatorA, Set<String> variablesB, ExprEvaluator evaluatorB) {
        // Take the intersection of the two branches
        Set<String> commonVariables = new HashSet<>(variablesA);
        commonVariables.retainAll(variablesB);

        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr resultA;
        IExpr resultB;

        for (String variable : commonVariables) {
            resultA = evaluatorA.getVariable(variable);
            resultB = evaluatorB.getVariable(variable);

            if (resultA == resultB) {
                // Both branches result in the same thing
                evaluator.defineVariable(variable, resultA);
            } else {
                // One branch diverges, so we just make the variable symbolic
                evaluator.defineVariable(variable);
            }

        }

        return evaluator;
    }


    private Set<String> deepcopyVariableNames() {
        Set<String> oldVariables = this.variableNameStack.peek();

        return new HashSet<>(oldVariables);
    }

    private ExprEvaluator deepcopyEvaluator() {
        ExprEvaluator newEvaluator = new ExprEvaluator();

        ExprEvaluator oldEvaluator = this.variableStack.peek();
        Set<String> variables = this.variableNameStack.peek();

        for (String variable : variables) {
            IExpr result = oldEvaluator.getVariable(variable);
            newEvaluator.defineVariable(variable, result);
        }

        return newEvaluator;
    }

    private boolean areAllIncomingPathsDead() {
        // Suppose we have ten incoming edges to this one node
        // As long as ONE edge is not dead, then we can say that this path is still viable, and we return false
        // If ALL incoming edges are dead, then we can say that this path is dead, and we return true

        // Stack empty means we can't make a determination that all paths are dead
        if (this.statementStack.empty()) {
            return false;
        }

        List<GraphNodeTuple> incoming = this.statementStack.peek();

        for (GraphNodeTuple gnt : incoming) {
            if (!gnt.pathToChildIsDead) {
                return false;
            }
        }

        return true;
    }

    /**
     * Resets 'continue called' state in symja back to default false.
     */
    private void ResetContinueVariableAfterLoop(ExprEvaluator eval) {
        // TODO: manually test to see if this brings any problems. (eg. nested loop + if + loop + continue + if +
        //  loop)
        eval.defineVariable(continueCalledStringName, false);
    }

}

class GraphNodeTuple {
    public GraphNode node;
    public String textFromParentToChild;
    public boolean pathToChildIsDead;

    public GraphNodeTuple(GraphNode node, boolean childIsDead) {
        this.node = node;
        this.textFromParentToChild = "";
        this.pathToChildIsDead = childIsDead;
    }

    public GraphNodeTuple(GraphNode node, String textFromParentToChild, boolean childIsDead) {
        this.node = node;
        this.textFromParentToChild = textFromParentToChild;
        this.pathToChildIsDead = childIsDead;
    }
}