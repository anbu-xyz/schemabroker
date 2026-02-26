package uk.anbu.schemabroker.bdd.leases;

import io.cucumber.spring.ScenarioScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import uk.anbu.schemabroker.model.SchemaLease;

/**
 * Scenario-scoped shared state for lease-related steps.
 */
@Setter
@Getter
@Component
@ScenarioScope
public class LeaseScenarioState {

    private SchemaLease existingLease;

}

