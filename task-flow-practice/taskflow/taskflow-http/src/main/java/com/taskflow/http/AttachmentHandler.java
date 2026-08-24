package com.taskflow.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.taskflow.domain.Attachment;
import com.taskflow.domain.User;
import com.taskflow.domain.AttachmentRepository;
import com.taskflow.util.FileUploadUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles attachment routes nested under {@code /tasks/{taskId}/attachments}.
 *
 * <p>Route table:
 * <ul>
 *   <li>POST /tasks/{taskId}/attachments — multipart/form-data, max 10 MB → 201 + metadata JSON</li>
 *   <li>GET  /tasks/{taskId}/attachments → 200 + metadata array JSON</li>
 * </ul>
 *
 * <p>This class is NOT registered as its own HttpServer context. Instead it is called from
 * {@link CommentHandler#handle} when the path segment at index 3 equals "attachments".
 */
public class AttachmentHandler {

    private static final long MAX_CONTENT_LENGTH = FileUploadUtil.MAX_BYTES;

    private final AttachmentRepository attachmentDao;
    private final ObjectMapper mapper;

    public AttachmentHandler(AttachmentRepository attachmentDao, ObjectMapper mapper) {
        this.attachmentDao = attachmentDao;
        this.mapper = mapper;
    }

    /**
     * Dispatches POST or GET for {@code /tasks/{taskId}/attachments}.
     */
    public void handle(HttpExchange exchange, long taskId) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        switch (method) {
            case "POST" -> handleUpload(exchange, taskId);
            case "GET"  -> handleList(exchange, taskId);
            default     -> AuthHandler.sendError(exchange, 405, "Method not allowed");
        }
    }

    // -------------------------------------------------------------------------
    // POST /tasks/{taskId}/attachments
    // -------------------------------------------------------------------------

    private void handleUpload(HttpExchange exchange, long taskId) throws IOException {
        // 1. Size check via Content-Length before reading body
        String contentLengthHeader = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLengthHeader != null) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader);
                if (contentLength > MAX_CONTENT_LENGTH) {
                    AuthHandler.sendError(exchange, 413, "File exceeds maximum size of 10 MB");
                    return;
                }
            } catch (NumberFormatException ignored) {
                // malformed header — proceed; stream read will enforce limit
            }
        }

        // 2. Extract boundary from Content-Type
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("multipart/form-data")) {
            AuthHandler.sendError(exchange, 400, "Content-Type must be multipart/form-data");
            return;
        }

        String boundary = null;
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                boundary = part.substring("boundary=".length()).trim();
                break;
            }
        }

        if (boundary == null || boundary.isBlank()) {
            AuthHandler.sendError(exchange, 400, "multipart boundary missing from Content-Type");
            return;
        }

        // 3. Parse multipart body
        FileUploadUtil.ParsedPart part;
        try {
            part = FileUploadUtil.parseMultipart(exchange.getRequestBody(), boundary);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("10 MB")) {
                AuthHandler.sendError(exchange, 413, msg);
            } else {
                AuthHandler.sendError(exchange, 400, msg != null ? msg : "Invalid multipart body");
            }
            return;
        }

        // 4. Sanitise filename and build stored name
        String safeOriginalName = FileUploadUtil.sanitiseFilename(part.filename());
        String storedName = UUID.randomUUID() + "_" + safeOriginalName;
        Path targetPath = Path.of("uploads", storedName);

        // 5. Write file to disk
        Files.write(targetPath, part.content());

        // 6. Persist metadata
        User user = (User) exchange.getAttribute(AuthFilter.ATTR_USER);

        Attachment attachment = new Attachment();
        attachment.setOriginalName(safeOriginalName);
        attachment.setStoredName(storedName);
        attachment.setMimeType(part.mimeType());
        attachment.setSizeBytes(part.content().length);
        attachment.setTaskId(taskId);
        attachment.setUploadedBy(user.getId());
        attachment.setCreatedAt(Instant.now());

        Attachment saved = attachmentDao.insert(attachment);
        AuthHandler.sendResponse(exchange, 201, mapper.writeValueAsString(saved));
    }

    // -------------------------------------------------------------------------
    // GET /tasks/{taskId}/attachments
    // -------------------------------------------------------------------------

    private void handleList(HttpExchange exchange, long taskId) throws IOException {
        List<Attachment> attachments = attachmentDao.findByTaskId(taskId);
        AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(attachments));
    }
}
