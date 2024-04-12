package viz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class GraphApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphApplication.class, args);
    }

    // code referenced from https://spring.io/guides/gs/rest-service-cors
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/graph").allowedOrigins("http://localhost:8080");
            }
        };
    }
}
