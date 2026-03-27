package uk.anbu.schemabroker.module1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.anbu.schemabroker.module1.model.Note;

public interface NoteRepository extends JpaRepository<Note, Long> {
}

