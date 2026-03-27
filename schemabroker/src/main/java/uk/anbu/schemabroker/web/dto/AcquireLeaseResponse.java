package uk.anbu.schemabroker.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class AcquireLeaseResponse {
    private final String leaseId;
    private final String schema;
    private final Instant expiresAt;
    private final long ttlSeconds;
}

