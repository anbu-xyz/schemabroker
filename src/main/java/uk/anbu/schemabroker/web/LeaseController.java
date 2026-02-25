package uk.anbu.schemabroker.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.web.dto.AcquireLeaseRequest;
import uk.anbu.schemabroker.web.dto.AcquireLeaseResponse;
import uk.anbu.schemabroker.model.SchemaLease;

@RestController
@RequestMapping("/api/v1/leases")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @PostMapping
    public ResponseEntity<?> acquireLease(@RequestBody AcquireLeaseRequest req) {
        var maybe = leaseService.acquireLease(req.getOwner(), req.getMetadata() == null ? null : req.getMetadata().toString());
        if (maybe.isPresent()) {
            SchemaLease lease = maybe.get();
            AcquireLeaseResponse resp = new AcquireLeaseResponse(lease.getLeaseId(), lease.getSchemaName(), lease.getExpiresAt(), leaseService.getTtlSeconds());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Collections.singletonMap("error", "no free schemas"));
        }
    }
}

