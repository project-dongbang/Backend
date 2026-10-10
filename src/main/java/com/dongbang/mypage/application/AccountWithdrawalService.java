package com.dongbang.mypage.application;

import com.dongbang.auth.domain.repository.AuthSessionRepository;
import com.dongbang.auth.domain.repository.OAuthAccountRepository;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.notification.domain.repository.NotificationRepository;
import com.dongbang.organization.application.port.FutureEventRegistrationCleanupPort;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.user.domain.User;
import com.dongbang.user.domain.UserStatus;
import com.dongbang.user.domain.repository.UserRepository;
import com.dongbang.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountWithdrawalService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final AuthSessionRepository authSessionRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final NotificationRepository notificationRepository;
    private final FutureEventRegistrationCleanupPort eventPort;
    private final FileStorageService fileStorageService;

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .filter(item -> item.getStatus() != UserStatus.WITHDRAWN)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        var memberships = membershipRepository.findAllIncludingInactiveByUserId(userId);
        boolean ownsActiveOrganization = memberships.stream().anyMatch(membership ->
                membership.getStatus() == MembershipStatus.ACTIVE
                        && membership.getRole().isOwner()
                        && membership.getOrganization().getStatus() == OrganizationStatus.ACTIVE);
        if (ownsActiveOrganization) {
            throw new GeneralException(OrganizationErrorCode.OWNER_CANNOT_LEAVE);
        }

        Instant withdrawnAt = Instant.now();
        String profileImageKey = user.getProfileImageStorageKey();
        memberships.forEach(membership -> eventPort.cancelFutureRegistrations(
                membership.getId(), membership.getOrganization().getId(), withdrawnAt));
        memberships.forEach(membership -> membership.anonymizeForAccountWithdrawal());
        notificationRepository.deleteAllByUserId(userId);
        authSessionRepository.deleteAllByUserId(userId);
        oauthAccountRepository.deleteAllByUserId(userId);
        user.withdraw(withdrawnAt);
        userRepository.flush();

        if (profileImageKey != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorageService.delete(profileImageKey);
                }
            });
        }
    }
}
