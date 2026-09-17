package com.dongbang.auth.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_oauth_accounts_provider_user",
                columnNames = {"provider", "provider_user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_account_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    private OAuthAccount(Long userId, OAuthProvider provider, String providerUserId, String providerEmail) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
    }

    public static OAuthAccount create(Long userId, OAuthProvider provider, String providerUserId, String providerEmail) {
        return new OAuthAccount(userId, provider, providerUserId, providerEmail);
    }

    public void updateProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }
}
