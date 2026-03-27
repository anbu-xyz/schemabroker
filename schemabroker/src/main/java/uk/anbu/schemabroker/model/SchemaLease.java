package uk.anbu.schemabroker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "schema_lease")
@Getter
@Setter
@NoArgsConstructor
public class SchemaLease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "lease_id", nullable = false, unique = true, length = 36)
    private String leaseId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "leased_at")
    private Instant leasedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "owner", length = 100)
    private String owner;

    @Lob
    @Column(name = "metadata")
    private String metadata;

}

