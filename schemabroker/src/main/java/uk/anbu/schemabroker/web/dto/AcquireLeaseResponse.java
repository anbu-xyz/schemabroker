package uk.anbu.schemabroker.web.dto;

import java.time.Instant;

public record AcquireLeaseResponse(String leaseId, String schema, String loginUser, String jdbcUrl, Instant expiresAt,
                                   long ttlSeconds) {
}

