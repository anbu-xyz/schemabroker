package uk.anbu.schemabroker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.anbu.schemabroker.model.SchemaPool;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SchemaPoolRepository extends JpaRepository<SchemaPool, Long> {

    Optional<SchemaPool> findBySchemaName(String schemaName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sp from SchemaPool sp where sp.enabled = true and sp.schemaName = :schemaName")
    Optional<SchemaPool> findBySchemaNameForUpdate(@Param("schemaName") String schemaName);

}

