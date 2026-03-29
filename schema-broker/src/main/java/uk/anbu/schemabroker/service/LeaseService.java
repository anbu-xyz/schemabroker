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
import uk.anbu.schemabroker.web.dto.LeaseListResponse;
import uk.anbu.schemabroker.web.dto.SchemaStatusDto;
import uk.anbu.schemabroker.web.dto.StatusResponse;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaseService {

    public static final String DEFAULT_GROUP_NAME = "default";

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
        expired.forEach(l -> {
            l.setStatus(LeaseStatus.EXPIRED);
            l.setExpiredAt(now);
            l.setExpiresAt(null);
        });
        leaseRepo.saveAll(expired);
        log.info("Expired {} leases", expired.size());
    }

    @Transactional
    public Optional<SchemaLease> acquireLease(String owner, String metadata,
                                              String clientIp, String clientHostname,
                                              Instant now) {
        return acquireLease(owner, metadata, clientIp, clientHostname, DEFAULT_GROUP_NAME, now);
    }

    @Transactional
    public Optional<SchemaLease> acquireLease(String owner, String metadata,
                                              String clientIp, String clientHostname,
                                              String groupName, Instant now) {
        // Find enabled pools
        var enabledPool = poolRepo.findAllByEnabledTrue();
        var eligiblePool = enabledPool.stream()
                .filter(p -> Objects.equals(p.getGroupName(), groupName))
                .toList();
        var defaultPool = enabledPool.stream()
                    .filter(p -> Objects.equals(p.getGroupName(), DEFAULT_GROUP_NAME))
                    .toList();
        List<SchemaPool> pools = new ArrayList<>(eligiblePool);
        Collections.shuffle(pools);
        pools.addAll(defaultPool);
        log.info("Number of available schemas: {}", pools.size());
        List<String> names = pools.stream().map(SchemaPool::getSchemaName).toList();
        log.info("Available schemas: {}", names);
        for (SchemaPool pool : pools) {
            // acquire lock
            var schemaForUpdate = poolRepo.findSchemaForUpdate(pool.getSchemaName(), pool.getJdbcUrl());
            if (schemaForUpdate.isEmpty()) {
                log.warn("Schema {}@{} disappeared!", pool.getSchemaName(), pool.getJdbcUrl());
                continue;
            }
            // Check if there's an active lease for this schema
            Optional<SchemaLease> active = leaseRepo.findActive(pool.getSchemaName(), pool.getJdbcUrl());
            if (active.isPresent()) {
                continue;
            }
            // Create lease
            var lease = SchemaLease.builder()
                    .schemaName(pool.getSchemaName())
                    .jdbcUrl(pool.getJdbcUrl())
                    .loginUser(pool.getLoginUser())
                    .leaseId(UUID.randomUUID().toString())
                    .status(LeaseStatus.ACTIVE)
                    .leasedAt(now)
                    .expiresAt(now.plusSeconds(ttlSeconds))
                    .lastHeartbeatAt(now)
                    .owner(owner)
                    .ipAddress(clientIp)
                    .hostname(clientHostname)
                    .metadata(metadata)
                    .build();
            var saved = Optional.of(leaseRepo.save(lease));
            log.info("Acquired schema: {} for owner: {}", pool.getSchemaName(), owner);
            return saved;
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

        if (!LeaseStatus.ACTIVE.equals(lease.getStatus()) || lease.getExpiresAt().isBefore(now)) {
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
        if (LeaseStatus.ACTIVE.equals(lease.getStatus())) {
            lease.setStatus(LeaseStatus.RELEASED);
            lease.setReleasedAt(Instant.now());
            lease.setExpiresAt(null);
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
                    pool.getGroupName(),
                    pool.getLoginUser(),
                    pool.getJdbcUrl(),
                    true,
                    "LEASED",
                    lease.getLeaseId(),
                    lease.getExpiresAt(),
                    lease.getOwner()
            );
        } else {
            return new SchemaStatusDto(
                    pool.getSchemaName(),
                    pool.getGroupName(),
                    pool.getLoginUser(),
                    pool.getJdbcUrl(),
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

    @Transactional(readOnly = true)
    public LeaseListResponse listAllLeases(Instant now) {
        return new LeaseListResponse(
                leaseRepo.findActiveLeasesNotExpired(now),
                leaseRepo.findActiveExpired(now),
                leaseRepo.findNonActive()
        );
    }
}
