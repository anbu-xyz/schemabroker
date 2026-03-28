package uk.anbu.schemabroker.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.service.LeaseStatus;
import uk.anbu.schemabroker.web.dto.AcquireLeaseRequest;
import uk.anbu.schemabroker.web.dto.AcquireLeaseResponse;
import uk.anbu.schemabroker.web.dto.ReleaseLeaseResponse;
import uk.anbu.schemabroker.model.SchemaLease;

import java.time.Instant;
import java.util.Optional;

import static uk.anbu.schemabroker.service.LeaseService.DEFAULT_GROUP_NAME;

@RestController
@RequestMapping("/api/v1/leases")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @PostMapping
    public ResponseEntity<?> acquireLease(@RequestBody AcquireLeaseRequest req, HttpServletRequest request) {
        var now = Instant.now();
        var clientIp = request.getRemoteAddr();
        var clientHostname = request.getRemoteHost();
        var groupName = req.getGroupName() == null? DEFAULT_GROUP_NAME : req.getGroupName();
        var maybe = leaseService.acquireLease(req.getOwner(), req.getMetadata() == null ? null : req.getMetadata().toString(),
                clientIp, clientHostname, groupName, now);
        if (maybe.isPresent()) {
            SchemaLease lease = maybe.get();
            AcquireLeaseResponse resp = new AcquireLeaseResponse(lease.getLeaseId(), lease.getSchemaName(),
                    lease.getLoginUser(), lease.getJdbcUrl(),
                    lease.getExpiresAt(), leaseService.getTtlSeconds());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Collections.singletonMap("error", "no free schemas"));
        }
    }

    @PostMapping("/{leaseId}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String leaseId) {
        Optional<SchemaLease> maybe = leaseService.heartbeat(leaseId, Instant.now());
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", "lease not found"));
        }
        SchemaLease lease = maybe.get();
        if (!LeaseStatus.ACTIVE.equals(lease.getStatus()) || lease.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(java.util.Collections.singletonMap("error", "lease expired or released"));
        }
        AcquireLeaseResponse resp = new AcquireLeaseResponse(lease.getLeaseId(), lease.getSchemaName(),
                lease.getLoginUser(), lease.getJdbcUrl(),
                lease.getExpiresAt(), leaseService.getTtlSeconds());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{leaseId}/release")
    public ResponseEntity<?> release(@PathVariable String leaseId) {
        Optional<SchemaLease> maybe = leaseService.release(leaseId);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", "lease not found"));
        }
        SchemaLease lease = maybe.get();
        ReleaseLeaseResponse resp = new ReleaseLeaseResponse(lease.getLeaseId(), lease.getSchemaName(), lease.getStatus().name());
        return ResponseEntity.ok(resp);
    }
}
