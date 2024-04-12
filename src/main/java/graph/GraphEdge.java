package graph;

import java.util.Objects;

public class GraphEdge {
    protected GraphNode node;
    public String label;
    public Boolean isDead;

    public GraphEdge(GraphNode n, String l, Boolean d ) {
        this.node = n;
        this.label = l;
        this.isDead = d;
    }

    public GraphNode getNode() {
        return node;
    }

    public String getLabel() {
        return label;
    }

    public Boolean getDead() {
        return isDead;
    }

    public boolean equalLabelAndIsDead(GraphEdge ge) {
        return this.label.equals(ge.label) && this.isDead.equals(ge.isDead);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(node, graphEdge.node) && Objects.equals(label, graphEdge.label) && Objects.equals(isDead, graphEdge.isDead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, label, isDead);
    }
}
