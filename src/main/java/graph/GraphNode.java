package graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GraphNode {
    private final UUID uuid;
    public BlockType blockType;
    public String text;
    public List<GraphEdge> edges;

    public GraphNode(BlockType bt, String t) {
        this.uuid = UUID.randomUUID();
        this.blockType = bt;
        this.text = t;
        this.edges = new ArrayList<>();
    }

    public void addEdge(GraphEdge c) {
        this.edges.add(c);
    }

    public GraphNode getChild(int index) {
        return this.edges.get(index).getNode();
    }

    public boolean equalTextAndType(GraphNode gn) {
        return this.text.equals(gn.text) && this.blockType.equals(gn.blockType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return blockType == graphNode.blockType && Objects.equals(text, graphNode.text) && Objects.equals(edges, graphNode.edges) && Objects.equals(uuid, graphNode.uuid);
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "blockType=" + blockType +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
