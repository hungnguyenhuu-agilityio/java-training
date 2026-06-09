package com.taskflow.util;

import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {

    private String export(List<Task> tasks) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvExporter.export(tasks, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void emptyList_returnsHeaderOnly() throws IOException {
        String csv = export(List.of());
        String[] lines = csv.split("\r\n|\n");
        assertEquals(1, lines.length);
        assertEquals("id,title,status,priority,dueDate,projectId,createdAt", lines[0]);
    }

    @Test
    void singleTask_producesCorrectRow() throws IOException {
        Task t = new Task(1L, "Buy milk", null, TaskStatus.TODO, Priority.LOW,
                LocalDate.of(2026, 1, 15), 7L, 3L,
                Instant.parse("2026-01-01T00:00:00Z"));
        String csv = export(List.of(t));
        String[] lines = csv.split("\r\n|\n");
        assertEquals(2, lines.length);
        assertEquals("1,Buy milk,TODO,LOW,2026-01-15,3,2026-01-01T00:00:00Z", lines[1]);
    }

    @Test
    void titleWithComma_isWrappedInQuotes() throws IOException {
        Task t = new Task(2L, "Buy milk, eggs", null, TaskStatus.TODO, Priority.MEDIUM,
                null, 1L, null, Instant.parse("2026-01-02T00:00:00Z"));
        String csv = export(List.of(t));
        String[] lines = csv.split("\r\n|\n");
        assertTrue(lines[1].startsWith("2,\"Buy milk, eggs\","), "Field with comma must be quoted");
    }

    @Test
    void titleWithQuote_escapesInnerQuotes() throws IOException {
        Task t = new Task(3L, "Buy \"organic\" milk", null, TaskStatus.DONE, Priority.HIGH,
                null, 1L, null, Instant.parse("2026-01-03T00:00:00Z"));
        String csv = export(List.of(t));
        String[] lines = csv.split("\r\n|\n");
        assertTrue(lines[1].contains("\"Buy \"\"organic\"\" milk\""),
                "Inner double-quotes must be escaped as double double-quotes");
    }

    @Test
    void nullOptionalFields_renderedAsEmpty() throws IOException {
        Task t = new Task(4L, "No due date", null, TaskStatus.TODO, Priority.LOW,
                null, 1L, null, Instant.parse("2026-01-04T00:00:00Z"));
        String csv = export(List.of(t));
        String[] lines = csv.split("\r\n|\n");
        // dueDate and projectId should be empty
        String row = lines[1];
        String[] cols = row.split(",", -1);
        assertEquals("", cols[4], "dueDate column should be empty when null");
        assertEquals("", cols[5], "projectId column should be empty when null");
    }
}
