package graph;

import java.util.List;
import java.util.Stack;

public class StateWrapper {
    public List<GraphNode> rootNodes;
    /**
     * Keeps track of loop conditionals for keyword 'continue' to link back to.
     */
    public Stack<LoopState> loopStates;

    public StateWrapper(List<GraphNode> rootNodes, Stack<LoopState> loopStates) {
        this.rootNodes = rootNodes;
        this.loopStates = loopStates;
    }
}


