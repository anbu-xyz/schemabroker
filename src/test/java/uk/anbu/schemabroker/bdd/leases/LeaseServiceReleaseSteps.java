package uk.anbu.schemabroker.bdd.leases;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.model.SchemaPool;
import uk.anbu.schemabroker.repository.SchemaLeaseRepository;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;
import uk.anbu.schemabroker.service.LeaseService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LeaseServiceReleaseSteps {

    @Autowired
    private LeaseService leaseService;

    @Autowired
    private SchemaPoolRepository schemaPoolRepository;

    @Autowired
    private SchemaLeaseRepository schemaLeaseRepository;

    @Autowired
    private LeaseScenarioState leaseScenarioState;

    private Optional<SchemaLease> releaseResult;

    @Given("there is an active lease for schema {string} owned by {string}")
    public void there_is_an_active_lease_for_schema_owned_by(String schemaName, String owner) {
        SchemaPool pool = schemaPoolRepository.findAll().stream()
                .filter(p -> p.getSchemaName().equals(schemaName))
                .findFirst()
                .orElseThrow();
        Instant now = Instant.now();
        SchemaLease lease = new SchemaLease();
        lease.setSchemaName(pool.getSchemaName());
        lease.setStatus("ACTIVE");
        lease.setLeasedAt(now);
        lease.setExpiresAt(now.plusSeconds(600));
        lease.setLastHeartbeatAt(now);
        lease.setLeaseId(UUID.randomUUID().toString());
        lease.setOwner(owner);
        leaseScenarioState.setExistingLease(schemaLeaseRepository.save(lease));
    }

    @Given("there is a released lease for schema {string} owned by {string}")
    public void there_is_a_released_lease_for_schema_owned_by(String schemaName, String owner) {
        SchemaPool pool = schemaPoolRepository.findAll().stream()
                .filter(p -> p.getSchemaName().equals(schemaName))
                .findFirst()
                .orElseThrow();
        Instant now = Instant.now();
        SchemaLease lease = new SchemaLease();
        lease.setSchemaName(pool.getSchemaName());
        lease.setStatus("RELEASED");
        lease.setLeasedAt(now.minusSeconds(600));
        lease.setExpiresAt(now.plusSeconds(600));
        lease.setLastHeartbeatAt(now.minusSeconds(600));
        lease.setLeaseId(UUID.randomUUID().toString());
        lease.setOwner(owner);
        leaseScenarioState.setExistingLease(schemaLeaseRepository.save(lease));
    }

    @When("I release that lease")
    public void i_release_that_lease() {
        releaseResult = leaseService.release(leaseScenarioState.getExistingLease().getLeaseId());
    }

    @When("I release a random lease id")
    public void i_release_a_random_lease_id() {
        releaseResult = leaseService.release("non-existent-" + UUID.randomUUID());
    }

    @Then("the released lease status is {string}")
    public void the_released_lease_status_is(String status) {
        assertThat(releaseResult).isPresent();
        assertThat(releaseResult.get().getStatus()).isEqualTo(status);
    }

    @Then("no released lease is returned")
    public void no_released_lease_is_returned() {
        assertThat(releaseResult).isEmpty();
    }
}
