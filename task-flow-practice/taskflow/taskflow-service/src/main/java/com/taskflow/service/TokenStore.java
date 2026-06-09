package com.taskflow.service;

import com.taskflow.domain.TokenEntry;
import com.taskflow.domain.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory token store backed by a {@link ConcurrentHashMap}.
 *
 * <p>Tokens are UUID strings with a 24-hour TTL.
 * A {@link ScheduledExecutorService} purges expired tokens once per hour.
 * A JVM shutdown hook stops the executor cleanly.
 *
 * <p>Expiry check uses {@code computeIfPresent} for atomic read-and-remove,
 * eliminating the TOCTOU race of a naïve {@code get} + {@code remove}.
 */
public class TokenStore {

    static final long TTL_HOURS = 24;

    private final ConcurrentHashMap<String, TokenEntry> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public TokenStore() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "token-purge");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::purgeExpired, 1, 1, TimeUnit.HOURS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "token-store-shutdown"));
    }

    /**
     * Stores a new token for {@code user} with a 24-hour TTL.
     *
     * @return the generated UUID token string
     */
    public String put(User user) {
        String token = UUID.randomUUID().toString();
        map.put(token, new TokenEntry(user, Instant.now().plusSeconds(TTL_HOURS * 3600)));
        return token;
    }

    /**
     * Validates a token atomically using {@code computeIfPresent}.
     *
     * <p>If the token exists but has expired, it is removed atomically and
     * {@link Optional#empty()} is returned. No TOCTOU race.
     *
     * @return the associated {@link User} if the token is present and unexpired
     */
    public Optional<User> validate(String token) {
        User[] result = new User[1];

        map.computeIfPresent(token, (k, entry) -> {
            if (entry.isValid()) {
                result[0] = entry.getUser();
                return entry;   // keep in map
            }
            return null;        // returning null removes the entry atomically
        });

        return Optional.ofNullable(result[0]);
    }

    /**
     * Removes all tokens whose {@code expiresAt} is in the past.
     * Called by the scheduled executor and exposed for tests.
     */
    public void purgeExpired() {
        map.entrySet().removeIf(e -> !e.getValue().isValid());
    }

    /**
     * Test-only helper: forces a token's expiry into the past so tests can
     * verify TTL behaviour without sleeping.
     */
    public void expireForTest(String token) {
        map.computeIfPresent(token, (k, entry) -> {
            entry.setExpiresAt(Instant.now().minusSeconds(1));
            return entry;
        });
    }

    /** Shuts down the cleanup executor. Called by the JVM shutdown hook. */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
