package com.taskflow.service;

import com.taskflow.domain.User;
import com.taskflow.persistence.UserDao;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Optional;

/**
 * Business logic for user registration and login.
 *
 * <p>Passwords are hashed with BCrypt (work factor 12) before persistence.
 * Plain-text passwords are never stored or logged.
 */
public class UserService {

    private final UserDao userDao;
    private final TokenStore tokenStore;

    public UserService(UserDao userDao, TokenStore tokenStore) {
        this.userDao = userDao;
        this.tokenStore = tokenStore;
    }

    /**
     * Registers a new user.
     *
     * @param name     display name
     * @param email    must be unique
     * @param password plain-text password — hashed before storage
     * @return the persisted {@link User} (with generated id)
     * @throws IllegalArgumentException if email is already registered
     */
    public User register(String name, String email, String password) {
        if (userDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(0, name, email, hash, Instant.now());
        return userDao.insert(user);
    }

    /**
     * Authenticates a user and issues a session token.
     *
     * <p>BCrypt check is always run (even for unknown emails) as a constant-time
     * guard against user-enumeration timing attacks.
     *
     * @param email    the user's email
     * @param password the plain-text password to verify
     * @return an {@link Optional} containing the UUID token, or empty on failure
     */
    public Optional<String> login(String email, String password) {
        Optional<User> maybeUser = userDao.findByEmail(email);

        // Always check a hash to prevent timing-based user enumeration
        String hashToCheck = maybeUser
                .map(User::getPasswordHash)
                .orElse(BCrypt.hashpw("dummy", BCrypt.gensalt(12)));

        boolean valid = BCrypt.checkpw(password, hashToCheck);
        if (!valid || maybeUser.isEmpty()) {
            return Optional.empty();
        }

        String token = tokenStore.put(maybeUser.get());
        return Optional.of(token);
    }
}
