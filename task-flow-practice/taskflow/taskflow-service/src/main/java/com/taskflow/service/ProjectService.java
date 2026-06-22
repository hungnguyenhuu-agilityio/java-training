package com.taskflow.service;

import com.taskflow.domain.Project;
import com.taskflow.domain.ProjectHasTasksException;
import com.taskflow.domain.ProjectRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for Project CRUD.
 *
 * <p>Delegates persistence to {@link ProjectDao}. The checked
 * {@link ProjectHasTasksException} from {@link ProjectDao#delete(long)} is
 * propagated to callers (HTTP layer maps it to 409).
 */
public class ProjectService {

    private final ProjectRepository projectDao;

    public ProjectService(ProjectRepository projectDao) {
        this.projectDao = projectDao;
    }

    /**
     * Creates a new project owned by the given user.
     *
     * @param name        project name (must not be blank — enforced by HTTP layer)
     * @param description optional description
     * @param ownerId     authenticated user's id
     * @return the persisted project with generated id
     */
    public Project create(String name, String description, long ownerId) {
        Project project = new Project(0, name, description, ownerId, Instant.now());
        return projectDao.insert(project);
    }

    /**
     * Returns all projects.
     */
    public List<Project> getAll() {
        return projectDao.findAll();
    }

    /**
     * Returns a project by id, or empty if not found.
     */
    public Optional<Project> getById(long id) {
        return projectDao.findById(id);
    }

    /**
     * Updates an existing project's name and description.
     */
    public void update(Project project) {
        projectDao.update(project);
    }

    /**
     * Deletes a project by id.
     *
     * @throws ProjectHasTasksException if the project still has linked tasks
     */
    public void delete(long id) throws ProjectHasTasksException {
        projectDao.delete(id);
    }
}
