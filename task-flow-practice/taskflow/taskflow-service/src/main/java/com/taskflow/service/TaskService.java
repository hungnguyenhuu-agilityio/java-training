package com.taskflow.service;

import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import com.taskflow.persistence.TaskDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Business logic for Task operations.
 *
 * <p>Filtering is done in Java Streams (FR-008) — the DAO always returns all rows
 * and the service applies predicates. This satisfies the learning goal; a large-scale
 * system would push filters to SQL.
 */
public class TaskService {

    private final TaskDao taskDao;

    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    /**
     * Returns tasks matching the supplied query parameters.
     *
     * <p>Supported filter keys:
     * <ul>
     *   <li>{@code status}    — case-insensitive {@link TaskStatus} name</li>
     *   <li>{@code priority}  — case-insensitive {@link Priority} name</li>
     *   <li>{@code projectId} — numeric project FK</li>
     * </ul>
     * Unknown keys are silently ignored. An empty map returns all tasks.
     *
     * @param filters raw query parameters
     * @return filtered (or all) tasks; never null
     * @throws IllegalArgumentException if a known filter value is invalid (caught by handler)
     */
    public List<Task> getTasks(Map<String, String> filters) {
        Stream<Task> stream = taskDao.findAll().stream();

        String statusVal = filters.get("status");
        if (statusVal != null && !statusVal.isBlank()) {
            TaskStatus status = TaskStatus.valueOf(statusVal.toUpperCase());
            stream = stream.filter(t -> t.getStatus() == status);
        }

        String priorityVal = filters.get("priority");
        if (priorityVal != null && !priorityVal.isBlank()) {
            Priority priority = Priority.valueOf(priorityVal.toUpperCase());
            stream = stream.filter(t -> t.getPriority() == priority);
        }

        String projectIdVal = filters.get("projectId");
        if (projectIdVal != null && !projectIdVal.isBlank()) {
            long projectId = Long.parseLong(projectIdVal);
            stream = stream.filter(t -> t.getProjectId() != null && t.getProjectId() == projectId);
        }

        return stream.toList();
    }

    /**
     * Creates and persists a new task.
     */
    public Task createTask(Task task) {
        return taskDao.insert(task);
    }

    /**
     * Returns a task by id.
     */
    public Optional<Task> getTaskById(long id) {
        return taskDao.findById(id);
    }

    /**
     * Updates a task and returns it.
     *
     * @throws IllegalArgumentException if the task does not exist
     */
    public Task updateTask(Task task) {
        taskDao.findById(task.getId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + task.getId()));
        taskDao.update(task);
        return taskDao.findById(task.getId()).orElseThrow();
    }

    /**
     * Deletes a task by id.
     *
     * @return {@code true} if the task existed and was deleted
     */
    public boolean deleteTask(long id) {
        Optional<Task> existing = taskDao.findById(id);
        if (existing.isEmpty()) {
            return false;
        }
        taskDao.delete(id);
        return true;
    }
}
