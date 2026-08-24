package com.taskflow.util;

import com.taskflow.domain.Task;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports a list of {@link Task} objects to an RFC 4180 CSV stream.
 *
 * <p>Header: {@code id,title,status,priority,dueDate,projectId,createdAt}
 *
 * <p>The caller must NOT close the provided {@link OutputStream} — that is the
 * responsibility of the HTTP server (e.g. {@code HttpExchange} closes it after
 * the handler returns).
 */
public final class CsvExporter {

    private static final String HEADER = "id,title,status,priority,dueDate,projectId,createdAt";

    private CsvExporter() {}

    /**
     * Writes the CSV representation of {@code tasks} to {@code out}.
     *
     * @param tasks tasks to export (may be empty)
     * @param out   target stream — NOT closed by this method
     * @throws IOException on write failure
     */
    public static void export(List<Task> tasks, OutputStream out) throws IOException {
        // Do NOT wrap out in a try-with-resources; closing BufferedWriter closes the
        // underlying OutputStream, which HttpServer must close itself.
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(HEADER);
        writer.newLine();
        for (Task t : tasks) {
            writer.write(toCsvRow(t));
            writer.newLine();
        }
        writer.flush();
        // Intentionally NOT calling writer.close() — see javadoc above.
    }

    // -------------------------------------------------------------------------
    // Package-private for testing if needed; private is fine here.
    // -------------------------------------------------------------------------

    private static String toCsvRow(Task t) {
        return joinFields(
                String.valueOf(t.getId()),
                t.getTitle(),
                t.getStatus() != null ? t.getStatus().name() : "",
                t.getPriority() != null ? t.getPriority().name() : "",
                t.getDueDate() != null ? t.getDueDate().toString() : "",
                t.getProjectId() != null ? t.getProjectId().toString() : "",
                t.getCreatedAt() != null ? t.getCreatedAt().toString() : ""
        );
    }

    private static String joinFields(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    /**
     * RFC 4180 escaping: wrap the field in double-quotes if it contains a comma,
     * double-quote, or newline; escape inner double-quotes as {@code ""}.
     */
    static String escape(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!needsQuoting) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
