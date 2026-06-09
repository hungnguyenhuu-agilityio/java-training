package com.taskflow.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Utility for parsing multipart/form-data and sanitising uploaded filenames.
 *
 * <p>Java's built-in {@link com.sun.net.httpserver.HttpServer} has no multipart support,
 * so this class implements a minimal boundary-based parser sufficient for single-file uploads.
 */
public class FileUploadUtil {

    /** Maximum accepted upload size: 10 MB */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    /**
     * Immutable record holding one parsed multipart part.
     */
    public record ParsedPart(String filename, String mimeType, byte[] content) {}

    /**
     * Strips directory components from {@code rawName} to prevent path-traversal attacks.
     *
     * <p>Example: {@code "../../etc/passwd"} → {@code "passwd"}.
     */
    public static String sanitiseFilename(String rawName) {
        // Replace backslashes with forward slashes first (Windows-style paths)
        String normalised = rawName.replace('\\', '/');
        // Paths.get(...).getFileName() strips all leading directory segments
        return Path.of(normalised).getFileName().toString();
    }

    /**
     * Parses the first file part from a {@code multipart/form-data} body.
     *
     * <p>The entire body is read into memory; if it exceeds {@link #MAX_BYTES} an
     * {@link IllegalArgumentException} is thrown before any file is written to disk.
     *
     * @param body     raw request body stream
     * @param boundary the boundary token extracted from the {@code Content-Type} header
     *                 (without the leading {@code --})
     * @return the first parsed part
     * @throws IllegalArgumentException if the body is too large, the boundary is absent,
     *                                  or no file part is found
     * @throws IOException              on stream read errors
     */
    public static ParsedPart parseMultipart(InputStream body, String boundary)
            throws IOException {

        // Read body with size guard
        byte[] raw = readWithLimit(body);

        // Boundary delimiter as bytes
        byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

        // Split body on delimiter
        int[] start = indexOf(raw, delimiter, 0);
        if (start == null) {
            throw new IllegalArgumentException("Multipart boundary not found in request body");
        }

        // Move past the delimiter + CRLF
        int pos = start[1]; // position after the delimiter token
        // skip \r\n after opening delimiter
        if (pos < raw.length - 1 && raw[pos] == '\r' && raw[pos + 1] == '\n') {
            pos += 2;
        }

        // Parse part headers until blank line (\r\n\r\n)
        int headerEnd = indexOfBytes(raw, new byte[]{'\r', '\n', '\r', '\n'}, pos);
        if (headerEnd < 0) {
            throw new IllegalArgumentException("Malformed multipart: no header/body separator found");
        }

        String headers = new String(raw, pos, headerEnd - pos, StandardCharsets.ISO_8859_1);
        String filename = extractHeader(headers, "filename");
        String mimeType = extractContentType(headers);

        // Content starts after the \r\n\r\n
        int contentStart = headerEnd + 4;

        // Content ends at next --boundary
        byte[] closingDelimiter = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        int[] end = indexOf(raw, closingDelimiter, contentStart);
        int contentEnd = (end != null) ? end[0] : raw.length;

        byte[] content = Arrays.copyOfRange(raw, contentStart, contentEnd);
        return new ParsedPart(filename, mimeType, content);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static byte[] readWithLimit(InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int read;
        while ((read = is.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_BYTES) {
                throw new IllegalArgumentException(
                        "Upload exceeds maximum allowed size of 10 MB");
            }
        }
        return out.toByteArray();
    }

    /**
     * Returns the position [start, end] of {@code needle} in {@code haystack} starting from
     * {@code from}, or {@code null} if not found.
     */
    private static int[] indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = from; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return new int[]{i, i + needle.length};
        }
        return null;
    }

    private static int indexOfBytes(byte[] haystack, byte[] needle, int from) {
        int[] r = indexOf(haystack, needle, from);
        return r != null ? r[0] : -1;
    }

    /**
     * Extracts the {@code filename} attribute from a Content-Disposition header line.
     */
    private static String extractHeader(String headers, String attribute) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-disposition")) {
                for (String part : line.split(";")) {
                    part = part.trim();
                    if (part.startsWith(attribute + "=")) {
                        String value = part.substring(attribute.length() + 1).trim();
                        // Strip surrounding quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        return value;
                    }
                }
            }
        }
        return "upload";
    }

    /**
     * Extracts the Content-Type value from part headers, defaulting to
     * {@code application/octet-stream} if absent.
     */
    private static String extractContentType(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-type:")) {
                return line.substring("content-type:".length()).trim();
            }
        }
        return "application/octet-stream";
    }
}
