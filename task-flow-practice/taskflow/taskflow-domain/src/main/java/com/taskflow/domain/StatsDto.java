package com.taskflow.domain;

import java.util.Map;

/**
 * DTO for the dashboard statistics endpoint.
 *
 * <p>{@code byStatus} keys match {@link TaskStatus} names: "TODO", "IN_PROGRESS", "DONE".
 * {@code byPriority} keys match {@link Priority} names: "LOW", "MEDIUM", "HIGH".
 * Every enum value is always present with a count of at least 0.
 */
public class StatsDto {

    private Map<String, Long> byStatus;
    private Map<String, Long> byPriority;

    public StatsDto() {}

    public StatsDto(Map<String, Long> byStatus, Map<String, Long> byPriority) {
        this.byStatus = byStatus;
        this.byPriority = byPriority;
    }

    public Map<String, Long> getByStatus() { return byStatus; }
    public void setByStatus(Map<String, Long> byStatus) { this.byStatus = byStatus; }

    public Map<String, Long> getByPriority() { return byPriority; }
    public void setByPriority(Map<String, Long> byPriority) { this.byPriority = byPriority; }
}
