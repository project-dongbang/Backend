package com.dongbang.auth.infrastructure.persistence;

import com.dongbang.auth.domain.OAuthAccount;
import com.dongbang.auth.domain.repository.OAuthAccountRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccount, Long>, OAuthAccountRepository {
}
