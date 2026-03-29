package uk.anbu.schemabroker.web;

import static uk.anbu.schemabroker.service.LeaseService.DEFAULT_GROUP_NAME;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.service.LeaseStatus;
import uk.anbu.schemabroker.web.dto.AcquireLeaseRequest;
import uk.anbu.schemabroker.web.dto.AcquireLeaseResponse;
import uk.anbu.schemabroker.web.dto.ReleaseLeaseResponse;

@RestController
@RequestMapping("/api/v1/leases")
public class LeaseController {

    private static final ConcurrentHashMap<String, CachedHostname> HOSTNAME_CACHE =
        new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(60);

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @PostMapping
    public ResponseEntity<?> acquireLease(@RequestBody AcquireLeaseRequest req,
                                          HttpServletRequest request) {
        var now = Instant.now();
        var clientIp = request.getRemoteAddr();
        var clientHostname = resolveHostName(clientIp, request.getRemoteHost());
        var groupName = req.getGroupName() == null ? DEFAULT_GROUP_NAME : req.getGroupName();
        var maybe = leaseService.acquireLease(req.getOwner(),
            req.getMetadata() == null ? null : req.getMetadata().toString(),
            clientIp, clientHostname, groupName, now);
        if (maybe.isPresent()) {
            SchemaLease lease = maybe.get();
            AcquireLeaseResponse resp =
                new AcquireLeaseResponse(lease.getLeaseId(), lease.getSchemaName(),
                    lease.getLoginUser(), lease.getJdbcUrl(),
                    lease.getExpiresAt(), leaseService.getTtlSeconds());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Collections.singletonMap("error", "no free schemas"));
        }
    }

    private static String resolveHostName(String clientIp, String defaultName) {
        CachedHostname cached = HOSTNAME_CACHE.get(clientIp);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.hostname();
        }
        String hostname;
        try {
            hostname = InetAddress.getByName(clientIp).getHostName();
        } catch (java.net.UnknownHostException e) {
            hostname = defaultName;
        }
        HOSTNAME_CACHE.put(clientIp, new CachedHostname(hostname, Instant.now().plus(CACHE_TTL)));
        return hostname;
    }

    @PostMapping("/{leaseId}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String leaseId) {
        Optional<SchemaLease> maybe = leaseService.heartbeat(leaseId, Instant.now());
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(java.util.Collections.singletonMap("error", "lease not found"));
        }
        SchemaLease lease = maybe.get();
        if (!LeaseStatus.ACTIVE.equals(lease.getStatus())
            || lease.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.GONE)
                .body(java.util.Collections.singletonMap("error", "lease expired or released"));
        }
        AcquireLeaseResponse resp =
            new AcquireLeaseResponse(lease.getLeaseId(), lease.getSchemaName(),
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
        ReleaseLeaseResponse resp =
            new ReleaseLeaseResponse(lease.getLeaseId(), lease.getSchemaName(),
                lease.getStatus().name());
        return ResponseEntity.ok(resp);
    }

    private record CachedHostname(String hostname, Instant expiresAt) {
    }

}
