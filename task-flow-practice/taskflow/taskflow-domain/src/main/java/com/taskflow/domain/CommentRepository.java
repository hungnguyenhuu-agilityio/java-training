package com.taskflow.domain;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment insert(Comment comment);
    List<Comment> findByTaskId(long taskId);
    Optional<Comment> findById(long id);
    void delete(long commentId);
}
