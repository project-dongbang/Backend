package com.dongbang.user.infrastructure.persistence;

import com.dongbang.user.domain.User;
import com.dongbang.user.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
}
