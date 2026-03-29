package uk.anbu.schemabroker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.anbu.schemabroker.service.LeaseStatus;

@Entity
@Table(name = "schema_lease")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaLease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "login_user", nullable = false, length = 100)
    private String loginUser;

    @Column(name = "jdbc_url", length = 4000)
    private String jdbcUrl;

    @Column(name = "lease_id", nullable = false, unique = true, length = 36)
    private String leaseId;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LeaseStatus status;

    @Column(name = "leased_at")
    private Instant leasedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "hostname", nullable = false, length = 255)
    private String hostname;

    @Lob
    @Column(name = "metadata")
    private String metadata;

}

