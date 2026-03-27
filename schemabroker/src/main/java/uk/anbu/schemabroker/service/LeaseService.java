package uk.anbu.schemabroker.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.model.SchemaPool;
import uk.anbu.schemabroker.repository.SchemaLeaseRepository;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;
import uk.anbu.schemabroker.web.dto.SchemaStatusDto;
import uk.anbu.schemabroker.web.dto.StatusResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaseService {

    private final SchemaPoolRepository poolRepo;
    private final SchemaLeaseRepository leaseRepo;
    @Getter
    private final long ttlSeconds;

    public LeaseService(SchemaPoolRepository poolRepo, SchemaLeaseRepository leaseRepo,
                        @Value("${lease.ttlSeconds:600}") long ttlSeconds) {
        this.poolRepo = poolRepo;
        this.leaseRepo = leaseRepo;
        this.ttlSeconds = ttlSeconds;
    }

    // Runs every minute to expire ACTIVE leases past their expiry
    @Scheduled(fixedRateString = "PT1M")
    @Transactional
    public void expireLeases() {
        expireLeases(Instant.now());
    }

    @Transactional
    public void expireLeases(Instant now) {
        List<SchemaLease> expired = leaseRepo.findActiveExpired(now);
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(l -> l.setStatus("EXPIRED"));
        leaseRepo.saveAll(expired);
        log.info("Expired {} leases", expired.size());
    }

    @Transactional
    public Optional<SchemaLease> acquireLease(String owner, String metadata, Instant now) {
        // Find enabled pools
        List<SchemaPool> pools = poolRepo.findAll();
        for (SchemaPool pool : pools) {
            if (Boolean.FALSE.equals(pool.getEnabled())) {
                continue;
            }
            // Check if there's an active lease for this schema
            Optional<SchemaLease> active = leaseRepo.findActiveBySchemaName(pool.getSchemaName());
            if (active.isPresent()) {
                continue;
            }
            // Create lease
            SchemaLease lease = new SchemaLease();
            lease.setSchemaName(pool.getSchemaName());
            lease.setLeaseId(UUID.randomUUID().toString());
            lease.setStatus("ACTIVE");
            lease.setLeasedAt(now);
            lease.setExpiresAt(now.plusSeconds(ttlSeconds));
            lease.setLastHeartbeatAt(now);
            lease.setOwner(owner);
            lease.setMetadata(metadata);
            SchemaLease saved = leaseRepo.save(lease);
            return Optional.of(saved);
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<SchemaLease> heartbeat(String leaseId, Instant now) {
        Optional<SchemaLease> leaseOpt = leaseRepo.findByLeaseId(leaseId);
        if (leaseOpt.isEmpty()) {
            return Optional.empty();
        }

        SchemaLease lease = leaseOpt.get();

        if (!"ACTIVE".equals(lease.getStatus()) || lease.getExpiresAt().isBefore(now)) {
            return Optional.of(lease);
        }

        lease.setLastHeartbeatAt(now);
        lease.setExpiresAt(now.plusSeconds(ttlSeconds));
        return Optional.of(leaseRepo.save(lease));
    }

    @Transactional
    public Optional<SchemaLease> release(String leaseId) {
        Optional<SchemaLease> leaseOpt = leaseRepo.findByLeaseId(leaseId);
        if (leaseOpt.isEmpty()) {
            return Optional.empty();
        }

        SchemaLease lease = leaseOpt.get();
        if ("ACTIVE".equals(lease.getStatus())) {
            lease.setStatus("RELEASED");
            return Optional.of(leaseRepo.save(lease));
        }
        return Optional.of(lease);
    }

    @Transactional(readOnly = true)
    public StatusResponse getStatus(Instant now) {
        List<SchemaPool> pools = poolRepo.findAll();
        List<SchemaLease> activeLeases = leaseRepo.findActiveLeasesNotExpired(now);

        Map<String, SchemaLease> bySchema = activeLeases.stream()
                .collect(Collectors.toMap(SchemaLease::getSchemaName, l -> l, (a, b) -> a));

        List<SchemaStatusDto> schemas = pools.stream()
                .map(pool -> toSchemaStatusDto(pool, bySchema))
                .toList();

        return new StatusResponse(ttlSeconds, schemas);
    }

    private static SchemaStatusDto toSchemaStatusDto(SchemaPool pool,
                                                     Map<String, SchemaLease> bySchema) {
        SchemaLease lease = bySchema.get(pool.getSchemaName());
        boolean enabled = Boolean.TRUE.equals(pool.getEnabled());
        if (lease != null && enabled) {
            return new SchemaStatusDto(
                    pool.getSchemaName(),
                    true,
                    "LEASED",
                    lease.getLeaseId(),
                    lease.getExpiresAt(),
                    lease.getOwner()
            );
        } else {
            return new SchemaStatusDto(
                    pool.getSchemaName(),
                    enabled,
                    "FREE",
                    null,
                    null,
                    null
            );
        }
    }

    public Optional<SchemaLease> getLeaseDetails(String leaseId) {
        return leaseRepo.findByLeaseId(leaseId);
    }
}
