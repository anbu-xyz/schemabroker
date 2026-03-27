package uk.anbu.schemabroker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.anbu.schemabroker.model.SchemaLease;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SchemaLeaseRepository extends JpaRepository<SchemaLease, Long> {

    Optional<SchemaLease> findByLeaseId(String leaseId);

    @Query("select sl from SchemaLease sl where sl.status = 'ACTIVE' and sl.expiresAt > :now")
    List<SchemaLease> findActiveLeasesNotExpired(@Param("now") Instant now);

    @Query("select sl from SchemaLease sl where sl.status = 'ACTIVE' and sl.expiresAt <= :now")
    List<SchemaLease> findActiveExpired(@Param("now") Instant now);

    @Query("select sl from SchemaLease sl where sl.schemaName = :schemaName and sl.status = 'ACTIVE'")
    Optional<SchemaLease> findActiveBySchemaName(@Param("schemaName") String schemaName);

}
