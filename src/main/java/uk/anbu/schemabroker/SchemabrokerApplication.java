package uk.anbu.schemabroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchemabrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemabrokerApplication.class, args);
    }

}
