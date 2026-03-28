package uk.anbu.schemabroker.sample.note.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.anbu.schemabroker.sample.note.model.Note;

public interface NoteRepository extends JpaRepository<Note, Long> {
}

