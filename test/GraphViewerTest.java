import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class GraphViewerTest {

    // visual tests only
    @Test
    public void compileBasicGraph() {
        String filePath = "test\\testGraphs\\input\\basic.dot";
        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
            MutableGraph g = new Parser().read(fileContent); // Reads and a parses a .dot file
            Graphviz.fromGraph(g).width(700).render(Format.PNG).toFile(new File("test\\testGraphs\\output\\basic.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void compileDashedGraph() {
        String filePath = "test\\testGraphs\\input\\dashed.dot";
        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
            MutableGraph g = new Parser().read(fileContent); // Reads and a parses a .dot file
            Graphviz.fromGraph(g).width(700).render(Format.PNG).toFile(new File("test\\testGraphs\\output\\dash.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void compileColoredAndDashedGraph() {
        String filePath = "test\\testGraphs\\input\\coloredDashedGraph.dot";
        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
            MutableGraph g = new Parser().read(fileContent); // Reads and a parses a .dot file
            Graphviz.fromGraph(g).width(700).render(Format.PNG).toFile(new File("test\\testGraphs\\output\\coloredAndDashed.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void compileBasicMethod() {

    }

}