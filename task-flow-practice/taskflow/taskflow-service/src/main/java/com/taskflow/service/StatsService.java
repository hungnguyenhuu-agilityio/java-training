package com.taskflow.service;

import com.taskflow.domain.Priority;
import com.taskflow.domain.StatsDto;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import com.taskflow.persistence.TaskDao;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for computing dashboard statistics over all tasks.
 *
 * <p>Aggregation uses Java Streams ({@link Collectors#groupingBy} +
 * {@link Collectors#counting()}) — NOT SQL GROUP BY — to satisfy FR-008.
 *
 * <p>Every {@link TaskStatus} and {@link Priority} value is always present in the
 * returned maps with a default count of 0 so the caller never needs a null-check.
 */
public class StatsService {

    private final TaskDao taskDao;

    public StatsService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    /**
     * Returns task counts aggregated by status and priority.
     *
     * @return {@link StatsDto} with String-keyed maps (enum names) for JSON serialisation
     */
    public StatsDto getStats() {
        List<Task> tasks = taskDao.findAll();

        // Build EnumMap with 0 defaults so every value is always present
        EnumMap<TaskStatus, Long> statusCounts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            statusCounts.put(s, 0L);
        }

        EnumMap<Priority, Long> priorityCounts = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            priorityCounts.put(p, 0L);
        }

        // Aggregate via Streams (FR-008)
        tasks.stream()
                .filter(t -> t.getStatus() != null)
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()))
                .forEach(statusCounts::put);

        tasks.stream()
                .filter(t -> t.getPriority() != null)
                .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()))
                .forEach(priorityCounts::put);

        // Convert to String-keyed maps for Jackson serialisation
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s.name(), statusCounts.get(s));
        }

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            byPriority.put(p.name(), priorityCounts.get(p));
        }

        return new StatsDto(byStatus, byPriority);
    }
}
