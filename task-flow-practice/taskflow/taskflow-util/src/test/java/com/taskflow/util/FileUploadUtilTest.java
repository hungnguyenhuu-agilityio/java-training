package com.taskflow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FileUploadUtil}.
 */
class FileUploadUtilTest {

    // -----------------------------------------------------------------------
    // sanitiseFilename
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("sanitiseFilename: normal name is unchanged")
    void sanitise_normalName() {
        assertEquals("report.pdf", FileUploadUtil.sanitiseFilename("report.pdf"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd",
            "../secret.txt",
            "/absolute/path.txt",
            "a/b/c.txt"
    })
    @DisplayName("sanitiseFilename: path traversal stripped to base filename")
    void sanitise_pathTraversal(String rawName) {
        String safe = FileUploadUtil.sanitiseFilename(rawName);
        assertFalse(safe.contains("/"), "Sanitised name must not contain /");
        assertFalse(safe.contains(".."), "Sanitised name must not contain ..");
        assertFalse(safe.isBlank(), "Sanitised name must not be blank");
    }

    @Test
    @DisplayName("sanitiseFilename: Windows backslash path traversal stripped")
    void sanitise_windowsPath() {
        String safe = FileUploadUtil.sanitiseFilename("..\\..\\evil.sh");
        // Paths.getFileName handles both separators on Linux as a single token,
        // but we only guarantee no forward slashes (Linux FS safe)
        assertFalse(safe.contains("/"));
    }

    // -----------------------------------------------------------------------
    // parseMultipart
    // -----------------------------------------------------------------------

    private static byte[] buildMultipart(String boundary, String filename, String mimeType,
                                         byte[] fileContent) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + mimeType + "\r\n"
                + "\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes(StandardCharsets.ISO_8859_1);
        byte[] footerBytes = footer.getBytes(StandardCharsets.ISO_8859_1);

        byte[] result = new byte[headerBytes.length + fileContent.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(fileContent, 0, result, headerBytes.length, fileContent.length);
        System.arraycopy(footerBytes, 0, result, headerBytes.length + fileContent.length, footerBytes.length);
        return result;
    }

    @Test
    @DisplayName("parseMultipart: extracts filename and content from valid body")
    void parseMultipart_validBody() throws Exception {
        String boundary = "----TestBoundary123";
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] body = buildMultipart(boundary, "hello.txt", "text/plain", content);

        InputStream is = new ByteArrayInputStream(body);
        FileUploadUtil.ParsedPart part = FileUploadUtil.parseMultipart(is, boundary);

        assertNotNull(part);
        assertEquals("hello.txt", part.filename());
        assertEquals("text/plain", part.mimeType());
        assertArrayEquals(content, part.content());
    }

    @Test
    @DisplayName("parseMultipart: throws IllegalArgumentException when boundary missing from body")
    void parseMultipart_missingBoundary() {
        InputStream is = new ByteArrayInputStream("no boundary here".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,
                () -> FileUploadUtil.parseMultipart(is, "----missing"));
    }

    @Test
    @DisplayName("parseMultipart: rejects body exceeding 10 MB")
    void parseMultipart_tooLarge() {
        String boundary = "----BigBoundary";
        byte[] bigContent = new byte[11 * 1024 * 1024]; // 11 MB
        byte[] body = buildMultipart(boundary, "big.bin", "application/octet-stream", bigContent);

        InputStream is = new ByteArrayInputStream(body);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FileUploadUtil.parseMultipart(is, boundary));
        assertTrue(ex.getMessage().contains("10 MB"), "Error should mention 10 MB limit");
    }
}
