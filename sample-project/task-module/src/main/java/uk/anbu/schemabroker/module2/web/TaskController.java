package uk.anbu.schemabroker.module2.web;

import org.springframework.web.bind.annotation.*;
import uk.anbu.schemabroker.module2.model.Task;
import uk.anbu.schemabroker.module2.repository.TaskRepository;

import java.util.List;

@RestController
@RequestMapping("/api/task-module/tasks")
public class TaskController {

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public List<Task> list() {
        return taskRepository.findAll();
    }

    @PostMapping
    public Task create(@RequestBody Task task) {
        return taskRepository.save(task);
    }

    @PutMapping("/{id}/toggle")
    public Task toggle(@PathVariable Long id) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setCompleted(!task.isCompleted());
        return taskRepository.save(task);
    }
}

