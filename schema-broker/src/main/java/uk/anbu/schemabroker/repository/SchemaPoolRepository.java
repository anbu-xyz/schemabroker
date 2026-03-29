package uk.anbu.schemabroker.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.anbu.schemabroker.model.SchemaPool;

public interface SchemaPoolRepository extends JpaRepository<SchemaPool, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sp from SchemaPool sp "
            + "where sp.enabled = true "
            + "and sp.schemaName = :schemaName "
            + "and sp.jdbcUrl = :jdbcUrl")
    Optional<SchemaPool> findSchemaForUpdate(@Param("schemaName") String schemaName,
                                             @Param("jdbcUrl") String jdbcUrl);

    List<SchemaPool> findAllByEnabledTrue();

}

