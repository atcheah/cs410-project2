package viz;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import graph.GraphNode;
import graph.StateWrapper;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import visitor.MethodVisitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ProjectGenerator {

    public ProjectGenerator(String inputCode) {
        // Configure type solvers
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();

        // Combine the type solvers
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);

        // Configure symbol resolver with the combined type solver
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        try {
//            CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(FILE_PATH)));
            CompilationUnit cu = StaticJavaParser.parse(inputCode);
            Set<MethodDeclaration> methodDeclarations = new HashSet<>();
            List<TypeDeclaration<?>> types = cu.getTypes();
            for (int i = 0; i < types.size(); i++) {
                List<MethodDeclaration> mds = types.get(i).getMethods();
                methodDeclarations.addAll(mds);
            }

            List<GraphNode> nodes = new ArrayList<>();
            StateWrapper init = new StateWrapper(nodes, new Stack<>());
            VoidVisitor<StateWrapper> methodVisitor = new MethodVisitor(methodDeclarations);
            methodVisitor.visit(cu, init);

            System.out.println("=====================================================================");
            GraphGenerator generator = new GraphGenerator();
            String output = generator.generateFullGraph(nodes);
            System.out.println("=====================================================================");
            try {
                MutableGraph g = new Parser().read(output); // Reads and a parses a .dot file
                Graphviz.fromGraph(g).render(Format.SVG).toFile(new File("src/main/java/viz/generatedGraph.svg"));
            } catch (IOException e) {
                System.out.println("Error in generating graph");
            }
            System.out.println("Generation of graph is complete");
            System.out.println("=====================================================================");


        } catch (Exception e) {
            System.out.println("Error in parsing graph");
//            e.printStackTrace();
        }
    }
}
