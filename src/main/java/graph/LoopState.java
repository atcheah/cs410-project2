package graph;

/**
 * Keeps track of whether loop body path nodes are currently dead or not.
 */
public class LoopState {
    public GraphNode loopConditional;
    // eg. i++ or iterator.next().
    public GraphNode loopUpdater;
    public boolean containsContinue;

    public LoopState(GraphNode loopConditional, GraphNode loopUpdater, boolean containsContinue) {
        this.loopConditional = loopConditional;
        this.loopUpdater = loopUpdater;
        this.containsContinue = containsContinue;
    }
}