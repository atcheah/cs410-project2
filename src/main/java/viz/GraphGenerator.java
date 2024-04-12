package viz;

import graph.BlockType;
import graph.GraphEdge;
import graph.GraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GraphGenerator {

    // Generates a new graph file from the input
    String tab = "  ";
    String doubleTab = tab + tab;
    HashMap<GraphNode, String> nodeLabels;
    int nodeCounter;
    int clusterCounter;

    List<GraphNode> visited;

    public GraphGenerator() {
        nodeCounter = 0;
        clusterCounter = 0;
        nodeLabels = new HashMap<>();
        visited = new ArrayList<>();
    }

    // generates a new label for a node
    private String generateNewNodeLabel(GraphNode node) {
        String newLabel = "node" + nodeCounter;
        nodeLabels.put(node, newLabel);
        nodeCounter++;
        return newLabel;
    }

    // generates a new label for a cluster of nodes i.e. subgraph
    private String generateNewClusterLabel() {
        String newLabel = "cluster" + clusterCounter;
        clusterCounter++;
        return newLabel;
    }

    // generates the definition for a node received from an existing edge
    private void generateDotDefinitions(GraphNode node, StringBuilder sb) {
        String nodeLabel = nodeLabels.get(node);
        generateDotEdges(node, sb, nodeLabel);
    }

    private void generateDotEdges(GraphNode node, StringBuilder sb, String nodeLabel) {
        if (node.edges.isEmpty()) {
            sb.append(doubleTab).append(nodeLabel).append(" -> ").append("end").append(clusterCounter).append("\n");
        } else {
            for (GraphEdge edge : node.edges) {
                String edgeNodeLabel = nodeLabels.containsKey(edge.getNode()) ? nodeLabels.get(edge.getNode()) : generateNewNodeLabel(edge.getNode());
                sb.append(doubleTab).append(edgeNodeLabel).append(" [label=\"").append(sanitizeText(edge.getNode().text)).append("\", shape=").append(blockTypeToShape(edge.getNode().blockType)).append("]\n");
                sb.append(doubleTab).append(nodeLabel).append(" -> ").append(edgeNodeLabel).append(" [label=\"").append(sanitizeText(edge.label)).append("\"");

                if (edge.isDead) {
                    sb.append(", style=dotted, color=red]\n");
                } else {
                    sb.append("]\n");
                }

                if (!visited.contains(edge.getNode())) {
                    visited.add(edge.getNode());
                    generateDotDefinitions(edge.getNode(), sb);
                }
            }
        }
    }

    // generates a subgraph for an individual function call
    private void generateSubGraph(GraphNode node, StringBuilder sb) {
        visited.add(node);
        // String nodeLabel = nodeLabels.containsKey(node) ? nodeLabels.get(node) : generateNewNodeLabel(node);

        sb.append(tab).append("subgraph ").append(generateNewClusterLabel()).append(" {\n");
        sb.append(doubleTab).append("label=\"").append(sanitizeText(node.text)).append("\"\n");
        sb.append(doubleTab).append("start").append(clusterCounter).append(" [shape = rectangle, style=filled, color=green, fontcolor=white, label=\"start\"]\n");
        sb.append(doubleTab).append("end").append(clusterCounter).append(" [shape = rectangle, style=filled, color=red, fontcolor=white, label=\"end\"]\n");

        generateDotEdges(node, sb, "start" + clusterCounter);
        sb.append(tab).append("}\n");
    }
    public String generateFullGraph(List<GraphNode> nodes) {
        StringBuilder output = new StringBuilder("digraph {\n");
        for (GraphNode node : nodes) {
            generateSubGraph(node, output);
            visited.clear();
        }
        output.append("}\n");
        return output.toString();
    }

    private String sanitizeText(String nodeText) {
        // replace any quotation marks with \"
        return nodeText.replaceAll("\"", "\\\\\"");
    }

    private String blockTypeToShape(BlockType type) {
        return switch (type) {
            case METHOD_DECL, STMT, METHOD_CALL, RETURN, BREAK -> "rectangles";
            case IF -> "diamond";
            case WHILE, FOR, FOREACH -> "invtrapezium";
            case SWITCH -> "house";
            default -> throw new IllegalArgumentException();
        };
    }
}
