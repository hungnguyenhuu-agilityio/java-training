package com.taskflow.domain;

import java.util.List;

public interface AttachmentRepository {
    Attachment insert(Attachment attachment);
    List<Attachment> findByTaskId(long taskId);
}
