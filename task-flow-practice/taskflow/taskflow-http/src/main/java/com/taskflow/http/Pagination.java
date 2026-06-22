package com.taskflow.http;

import com.sun.net.httpserver.HttpExchange;
import com.taskflow.domain.PageResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@code ?page} / {@code ?size} query parameters and slices a list into a {@link PageResponse}.
 *
 * <p>Defaults: page=0, size=20. Size is clamped to [1, 100].
 */
public final class Pagination {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private final int page;
    private final int size;

    private Pagination(int page, int size) {
        this.page = page;
        this.size = size;
    }

    /** Parses pagination params from the exchange's query string. */
    public static Pagination from(HttpExchange exchange) {
        Map<String, String> params = parseQueryString(exchange.getRequestURI().getQuery());
        int page = parseNonNegative(params.get("page"), 0);
        int size = clamp(parseNonNegative(params.get("size"), DEFAULT_SIZE));
        return new Pagination(page, size);
    }

    /** Parses pagination params from an already-parsed query map (handlers that parse their own params). */
    public static Pagination from(Map<String, String> params) {
        int page = parseNonNegative(params.get("page"), 0);
        int size = clamp(parseNonNegative(params.get("size"), DEFAULT_SIZE));
        return new Pagination(page, size);
    }

    /** Slices {@code all} according to this page/size and wraps it in a {@link PageResponse}. */
    public <T> PageResponse<T> apply(List<T> all) {
        long total = all.size();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageResponse<>(all.subList(from, to), page, size, total);
    }

    public int getPage() { return page; }
    public int getSize() { return size; }

    // -------------------------------------------------------------------------

    static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) params.put(kv[0].trim(), kv[1].trim());
        }
        return params;
    }

    private static int parseNonNegative(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            int v = Integer.parseInt(value.trim());
            return v >= 0 ? v : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int clamp(int size) {
        if (size <= 0) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }
}
