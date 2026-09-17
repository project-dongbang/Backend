package com.dongbang.user.domain.repository;

import com.dongbang.user.domain.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    boolean existsByEmail(String email);
    boolean existsByStudentNumber(String studentNumber);
    void flush();
}
