package uk.anbu.schemabroker.bdd.leases;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import uk.anbu.schemabroker.repository.SchemaLeaseRepository;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;

public class LeaseServiceScenarioHooks {

    @Autowired
    private SchemaLeaseRepository schemaLeaseRepository;

    @Autowired
    private SchemaPoolRepository schemaPoolRepository;

    @Before
    public void startScenario() {
        // Ensure a clean state before each scenario without using test-managed transactions
        schemaLeaseRepository.deleteAll();
        schemaPoolRepository.deleteAll();
    }

    @After
    public void afterScenario() {
        // Cleanup to keep scenarios isolated
        schemaLeaseRepository.deleteAll();
        schemaPoolRepository.deleteAll();
    }
}
