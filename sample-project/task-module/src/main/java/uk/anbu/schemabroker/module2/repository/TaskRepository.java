package uk.anbu.schemabroker.module2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.anbu.schemabroker.module2.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
}

