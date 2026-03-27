package uk.anbu.schemabroker.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.web.dto.LeaseListResponse;
import uk.anbu.schemabroker.web.dto.StatusResponse;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final LeaseService leaseService;

    public StatusController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @GetMapping("/schemas")
    public ResponseEntity<StatusResponse> getStatus() {
        Instant now = Instant.now();
        StatusResponse status = leaseService.getStatus(now);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/leases")
    public ResponseEntity<LeaseListResponse> listAllLeases() {
        Instant now = Instant.now();
        LeaseListResponse response = leaseService.listAllLeases(now);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<SchemaLease> getLeaseDetails(@PathVariable String leaseId) {
        return leaseService.getLeaseDetails(leaseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}