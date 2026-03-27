package uk.anbu.schemabroker.module1.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.anbu.schemabroker.module1.model.Note;
import uk.anbu.schemabroker.module1.repository.NoteRepository;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/note-module/notes")
public class NoteController {

    private final NoteRepository noteRepository;

    public NoteController(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @GetMapping
    public List<Note> list() {
        return noteRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Note> create(@RequestBody Note note) {
        Note saved = noteRepository.save(note);
        return ResponseEntity.created(URI.create("/api/note-module/notes/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!noteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        noteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

