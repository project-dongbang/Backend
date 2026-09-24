package com.dongbang.user.domain.repository;

import com.dongbang.user.domain.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    java.util.List<User> findAllByIdIn(java.util.Collection<Long> ids);
    boolean existsByEmail(String email);
    boolean existsByStudentNumber(String studentNumber);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByStudentNumberAndIdNot(String studentNumber, Long id);
    void flush();
}
