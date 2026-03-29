package uk.anbu.schemabroker.schema.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SchemaLease {

    private final String leaseId;
    private final String schema;
    private final String jdbcUrl;
    private final String loginUser;
    private final String loginPassword;
    private final Instant expiresAt;

    @JsonCreator
    public SchemaLease(
        @JsonProperty("leaseId") String leaseId,
        @JsonProperty("schema") String schema,
        @JsonProperty("jdbcUrl") String jdbcUrl,
        @JsonProperty("loginUser") String loginUser,
        @JsonProperty("loginPassword") String loginPassword,
        @JsonProperty("expiresAt") Instant expiresAt) {
        this.leaseId = leaseId;
        this.schema = schema;
        this.jdbcUrl = jdbcUrl;
        this.loginUser = loginUser;
        this.loginPassword = loginPassword;
        this.expiresAt = expiresAt != null ? expiresAt : Instant.now();
    }

    public String leaseId() {
        return leaseId;
    }

    public String schema() {
        return schema;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String loginUser() {
        return loginUser;
    }

    public String loginPassword() {
        return loginPassword;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

}

