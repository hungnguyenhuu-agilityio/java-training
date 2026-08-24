package com.taskflow.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Task insert(Task task);
    List<Task> findAll();
    Optional<Task> findById(long id);
    void update(Task task);
    void delete(long id);
    List<Task> findDueSoon(LocalDate from, LocalDate to);
}
