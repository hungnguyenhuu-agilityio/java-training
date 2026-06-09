package com.taskflow.service;

import com.taskflow.domain.Comment;
import com.taskflow.persistence.CommentDao;
import com.taskflow.persistence.TaskDao;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for {@link Comment}.
 *
 * <p>Validates task existence before add, and enforces author ownership on delete.
 */
public class CommentService {

    private final CommentDao commentDao;
    private final TaskDao taskDao;

    public CommentService(CommentDao commentDao, TaskDao taskDao) {
        this.commentDao = commentDao;
        this.taskDao = taskDao;
    }

    /**
     * Adds a comment to a task.
     *
     * @throws IllegalArgumentException if body is blank, body exceeds 5000 chars, or task does not exist
     */
    public Comment add(String body, long taskId, long authorId) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Comment body must not be blank");
        }
        if (body.length() > 5000) {
            throw new IllegalArgumentException("Comment body must not exceed 5000 characters");
        }
        taskDao.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        Comment c = new Comment(0, body.trim(), taskId, authorId, Instant.now());
        return commentDao.insert(c);
    }

    /**
     * Returns all comments for a task, ordered by creation time ascending.
     */
    public List<Comment> list(long taskId) {
        return commentDao.findByTaskId(taskId);
    }

    /**
     * Deletes a comment, enforcing author ownership.
     *
     * @throws IllegalArgumentException if the comment does not exist
     * @throws IllegalStateException    with message "Forbidden" if requestingUserId != comment.authorId
     */
    public void delete(long commentId, long requestingUserId) {
        Comment c = commentDao.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        if (c.getAuthorId() != requestingUserId) {
            throw new IllegalStateException("Forbidden");
        }
        commentDao.delete(commentId);
    }
}
