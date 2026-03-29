package uk.anbu.schemabroker.bdd.leases;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import uk.anbu.schemabroker.repository.SchemaLeaseRepository;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.web.dto.SchemaStatusDto;
import uk.anbu.schemabroker.web.dto.StatusResponse;

public class LeaseServiceStatusSteps {

    @Autowired
    private LeaseService leaseService;

    @Autowired
    private SchemaPoolRepository schemaPoolRepository;

    @Autowired
    private SchemaLeaseRepository schemaLeaseRepository;

    private StatusResponse statusResponse;

    @When("I request the lease service status")
    public void i_request_the_lease_service_status() {
        Instant now = Instant.now();
        statusResponse = leaseService.getStatus(now);
    }

    @Then("the status ttlSeconds equals the configured TTL")
    public void the_status_ttl_seconds_equals_the_configured_ttl() {
        assertThat(statusResponse.ttlSeconds()).isEqualTo(leaseService.getTtlSeconds());
    }

    @Then("status for schema {string} is {string} and enabled true")
    public void status_for_schema_is_and_enabled_true(String schemaName, String status) {
        Optional<SchemaStatusDto> dto = statusResponse.schemas().stream()
                .filter(s -> s.getSchema().equals(schemaName))
                .findFirst();
        assertThat(dto).isPresent();
        assertThat(dto.get().isEnabled()).isTrue();
        assertThat(dto.get().getStatus()).isEqualTo(status);
    }

    @Then("status for schema {string} is {string} and enabled false")
    public void status_for_schema_is_and_enabled_false(String schemaName, String status) {
        Optional<SchemaStatusDto> dto = statusResponse.schemas().stream()
                .filter(s -> s.getSchema().equals(schemaName))
                .findFirst();
        assertThat(dto).isPresent();
        assertThat(dto.get().isEnabled()).isFalse();
        assertThat(dto.get().getStatus()).isEqualTo(status);
    }
}
