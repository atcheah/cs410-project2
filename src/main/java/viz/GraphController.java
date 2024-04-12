package viz;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

@RestController
public class GraphController {

    private static final String FILE_PATH = "src/main/java/viz/generatedGraph.svg";

    @GetMapping("/hw")
    public String HelloWorld() {
        return "hello world";
    }

    // code references from https://stackoverflow.com/questions/59418029/how-to-use-spring-boot-java-to-properly-use-axios-post-from-react
    // and https://stackoverflow.com/questions/67497234/axios-post-request-to-springboot-backend
    // and https://stackoverflow.com/questions/62825338/how-to-send-image-as-response-in-spring-boot
    // and https://stackoverflow.com/questions/74240649/spring-boot-api-to-download-svg-file
    @PostMapping (value = "/graph")
    public ResponseEntity<Resource> generateGraph(@RequestBody Map<String, String> input) throws IOException {
        // System.out.println(input.get("inputCode"));
        try {
            ProjectGenerator projectGenerator = new ProjectGenerator(input.get("inputCode"));
            byte[] svgByteArray = Files.readAllBytes(Paths.get(FILE_PATH));
            HttpHeaders header = new HttpHeaders();
            header.add("Content-Type", "image/svg+xml");

            final ByteArrayResource inputStream = new ByteArrayResource(svgByteArray);

            Files.delete(Paths.get(FILE_PATH));

            return ResponseEntity.status(HttpStatus.OK).headers(header).body(inputStream);
        } catch (Exception e) {
            byte[] msgByteArray = e.getMessage().getBytes();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ByteArrayResource(msgByteArray));
        }


//        return "this is the text: " + input;
    }

}
