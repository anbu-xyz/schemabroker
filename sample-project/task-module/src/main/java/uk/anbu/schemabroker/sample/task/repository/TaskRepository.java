package uk.anbu.schemabroker.sample.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.anbu.schemabroker.sample.task.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
}

