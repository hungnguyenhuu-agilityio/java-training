package com.taskflow.http;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.taskflow.domain.User;
import com.taskflow.service.TokenStore;

import java.io.IOException;
import java.util.Optional;

/**
 * HTTP filter that enforces Bearer-token authentication on protected paths.
 *
 * <p>Checks the {@code Authorization: Bearer <token>} header.
 * Expiry is checked atomically via {@link TokenStore#validate(String)} which
 * uses {@code computeIfPresent} internally — no TOCTOU race (NFR-007).
 *
 * <p>On success the authenticated {@link User} is attached to the exchange as
 * a request attribute keyed by {@value ATTR_USER}, so downstream handlers can
 * retrieve it without re-validating.
 */
public class AuthFilter extends Filter {

    /** Attribute key used to attach the authenticated user to the exchange. */
    public static final String ATTR_USER = "authenticated_user";

    private final TokenStore tokenStore;

    public AuthFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public String description() {
        return "Bearer-token authentication filter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        AuthHandler.addCorsHeaders(exchange);

        // Always allow OPTIONS preflight through
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            AuthHandler.sendResponse(exchange, 204, "");
            return;
        }

        String header = exchange.getRequestHeaders().getFirst("Authorization");
        String token = extractToken(header);

        if (token == null) {
            AuthHandler.sendError(exchange, 401, "Missing or malformed Authorization header");
            return;
        }

        // Atomic expiry check — uses computeIfPresent inside TokenStore
        Optional<User> user = tokenStore.validate(token);
        if (user.isEmpty()) {
            AuthHandler.sendError(exchange, 401, "Token is missing, expired, or invalid");
            return;
        }

        exchange.setAttribute(ATTR_USER, user.get());
        chain.doFilter(exchange);
    }

    // -------------------------------------------------------------------------

    /**
     * Extracts the token from a {@code "Bearer <token>"} header value.
     *
     * @return the token string, or {@code null} if the header is absent/malformed
     */
    static String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
