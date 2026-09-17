package com.dongbang.auth.domain.repository;

import com.dongbang.auth.domain.OAuthAccount;
import com.dongbang.auth.domain.OAuthProvider;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository {
    OAuthAccount save(OAuthAccount account);
    void flush();
    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    List<OAuthAccount> findAllByUserId(Long userId);
}
