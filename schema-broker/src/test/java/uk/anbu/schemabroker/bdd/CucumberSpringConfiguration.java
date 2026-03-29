package uk.anbu.schemabroker.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.anbu.schemabroker.SchemabrokerApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = SchemabrokerApplication.class)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}