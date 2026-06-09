package com.taskflow.domain;

/**
 * Checked exception thrown when a Project cannot be deleted because it still
 * has Tasks associated with it (FK constraint {@code fk_tasks_project}).
 *
 * <p>Lives in {@code taskflow-domain} so it can be referenced by both
 * {@code taskflow-persistence} (where it is thrown) and
 * {@code taskflow-service} (where it is propagated to callers).
 * Maps to HTTP 409 Conflict in {@code ProjectHandler}.
 */
public class ProjectHasTasksException extends Exception {

    public ProjectHasTasksException(long projectId) {
        super("Project " + projectId + " still has tasks; reassign or delete them first");
    }
}
