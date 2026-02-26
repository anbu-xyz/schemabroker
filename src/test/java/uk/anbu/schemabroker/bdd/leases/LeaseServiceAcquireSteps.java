package uk.anbu.schemabroker.bdd.leases;

import io.cucumber.datatable.DataTable;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class LeaseServiceAcquireSteps {

    @Autowired
    private LeaseService leaseService;

    @Autowired
    private SchemaPoolRepository schemaPoolRepository;

    @Autowired
    private SchemaLeaseRepository schemaLeaseRepository;

    private Optional<SchemaLease> acquiredLease;

    @Given("the database is empty")
    public void the_database_is_empty() {
        schemaLeaseRepository.deleteAll();
        schemaPoolRepository.deleteAll();
    }

    @Given("the following schema pools exist:")
    public void the_following_schema_pools_exist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        for (Map<String, String> row : rows) {
            SchemaPool pool = new SchemaPool();
            pool.setSchemaName(row.get("schemaName"));
            pool.setEnabled(Boolean.parseBoolean(row.get("enabled")));
            schemaPoolRepository.save(pool);
        }
    }

    @Given("there are no active leases")
    public void there_are_no_active_leases() {
        schemaLeaseRepository.deleteAll();
    }

    @Given("each schema has an active non-expired lease")
    public void each_schema_has_an_active_non_expired_lease() {
        Instant now = Instant.now();
        for (SchemaPool pool : schemaPoolRepository.findAll()) {
            SchemaLease lease = new SchemaLease();
            lease.setSchemaName(pool.getSchemaName());
            lease.setStatus("ACTIVE");
            lease.setLeasedAt(now);
            lease.setExpiresAt(now.plusSeconds(600));
            lease.setLastHeartbeatAt(now);
            lease.setLeaseId("lease-" + pool.getSchemaName());
            lease.setOwner("seed-owner");
            schemaLeaseRepository.save(lease);
        }
    }

    @Given("there is an expired lease for schema {string}")
    public void there_is_an_expired_lease_for_schema(String schemaName) {
        Instant now = Instant.now();
        SchemaLease lease = new SchemaLease();
        lease.setSchemaName(schemaName);
        lease.setStatus("EXPIRED");
        lease.setLeasedAt(now.minusSeconds(1200));
        lease.setExpiresAt(now.minusSeconds(600));
        lease.setLastHeartbeatAt(now.minusSeconds(1200));
        lease.setLeaseId("expired-" + schemaName);
        lease.setOwner("seed-owner");
        schemaLeaseRepository.save(lease);
    }

    @When("a client {string} acquires a lease with metadata {string}")
    public void a_client_acquires_a_lease_with_metadata(String owner, String metadata) {
        acquiredLease = leaseService.acquireLease(owner, metadata);
    }

    @Then("a lease is returned")
    public void a_lease_is_returned() {
        assertThat(acquiredLease).isPresent();
    }

    @Then("no lease is returned")
    public void no_lease_is_returned() {
        assertThat(acquiredLease).isEmpty();
    }

    @Then("the lease status is {string}")
    public void the_lease_status_is(String status) {
        assertThat(acquiredLease).isPresent();
        assertThat(acquiredLease.get().getStatus()).isEqualTo(status);
    }

    @Then("the lease owner is {string}")
    public void the_lease_owner_is(String owner) {
        assertThat(acquiredLease).isPresent();
        assertThat(acquiredLease.get().getOwner()).isEqualTo(owner);
    }

    @Then("the lease schema is one of:")
    public void the_lease_schema_is_one_of(DataTable dataTable) {
        Set<String> allowed = dataTable.asList().stream().collect(Collectors.toSet());
        assertThat(acquiredLease).isPresent();
        assertThat(allowed).contains(acquiredLease.get().getSchemaName());
    }

    @Then("the lease schema is {string}")
    public void the_lease_schema_is(String schemaName) {
        assertThat(acquiredLease).isPresent();
        assertThat(acquiredLease.get().getSchemaName()).isEqualTo(schemaName);
    }
}
