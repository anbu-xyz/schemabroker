package uk.anbu.schemabroker.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.model.SchemaPool;
import uk.anbu.schemabroker.repository.SchemaLeaseRepository;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
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

    @Transactional
    public Optional<SchemaLease> acquireLease(String owner, String metadata) {
        // Find enabled pools
        List<SchemaPool> pools = poolRepo.findAll();
        Instant now = Instant.now();
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

}
