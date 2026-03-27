package uk.anbu.schemabroker.bdd.leases;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.repository.SchemaLeaseRepository;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.service.LeaseStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LeaseServiceHeartbeatSteps {

    @Autowired
    private LeaseService leaseService;

    @Autowired
    private SchemaPoolRepository schemaPoolRepository;

    @Autowired
    private SchemaLeaseRepository schemaLeaseRepository;

    @Autowired
    private LeaseScenarioState leaseScenarioState;

    private Instant rememberedExpiry;
    private Optional<SchemaLease> heartbeatResult;

    @Given("I remember the lease expiry time")
    public void i_remember_the_lease_expiry_time() {
        rememberedExpiry = leaseScenarioState.getExistingLease().getExpiresAt();
    }

    @When("I send a heartbeat for that lease")
    public void i_send_a_heartbeat_for_that_lease() {
        heartbeatResult = leaseService.heartbeat(leaseScenarioState.getExistingLease().getLeaseId(), Instant.now());
    }

    @When("I send a heartbeat for a random lease id")
    public void i_send_a_heartbeat_for_a_random_lease_id() {
        heartbeatResult = leaseService.heartbeat("non-existent-" + UUID.randomUUID(), Instant.now());
    }

    @Then("the heartbeat returns a lease")
    public void the_heartbeat_returns_a_lease() {
        assertThat(heartbeatResult).isPresent();
    }

    @Then("no heartbeat lease is returned")
    public void no_heartbeat_lease_is_returned() {
        assertThat(heartbeatResult).isEmpty();
    }

    @Then("the heartbeat lease status is {string}")
    public void the_heartbeat_lease_status_is(String status) {
        assertThat(heartbeatResult).isPresent();
        assertThat(heartbeatResult.get().getStatus()).isEqualTo(LeaseStatus.valueOf(status));
    }

    @Then("the lease expiry time is later than the remembered expiry time")
    public void the_lease_expiry_time_is_later_than_the_remembered_expiry_time() {
        assertThat(heartbeatResult).isPresent();
        assertThat(heartbeatResult.get().getExpiresAt())
                .isAfter(rememberedExpiry);
    }

    @Then("the lease expiry time is not later than the remembered expiry time")
    public void the_lease_expiry_time_is_not_later_than_the_remembered_expiry_time() {
        assertThat(heartbeatResult).isPresent();
        long actualMicros = heartbeatResult.get().getExpiresAt().getEpochSecond() * 1_000_000L
                + Math.round(heartbeatResult.get().getExpiresAt().getNano() / 1000.0);
        long expectedMicros = rememberedExpiry.getEpochSecond() * 1_000_000L
                + Math.round(rememberedExpiry.getNano() / 1000.0);
        assertThat(actualMicros).isLessThanOrEqualTo(expectedMicros);
    }
}
