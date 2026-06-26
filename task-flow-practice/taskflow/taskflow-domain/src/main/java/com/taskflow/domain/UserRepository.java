package com.taskflow.domain;

import java.util.Optional;

public interface UserRepository {
    User insert(User user);
    Optional<User> findByEmail(String email);
}
