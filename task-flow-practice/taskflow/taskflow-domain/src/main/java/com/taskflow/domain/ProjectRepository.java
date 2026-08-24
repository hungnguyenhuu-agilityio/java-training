package com.taskflow.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project insert(Project project);
    List<Project> findAll();
    Optional<Project> findById(long id);
    void update(Project project);
    void delete(long id) throws ProjectHasTasksException;
}
