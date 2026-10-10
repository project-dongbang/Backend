package com.dongbang.mypage.application;

import com.dongbang.auth.domain.repository.AuthSessionRepository;
import com.dongbang.auth.domain.repository.OAuthAccountRepository;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.notification.domain.repository.NotificationRepository;
import com.dongbang.organization.application.port.FutureEventRegistrationCleanupPort;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.user.domain.User;
import com.dongbang.user.domain.UserStatus;
import com.dongbang.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountWithdrawalServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private OAuthAccountRepository oauthAccountRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private FutureEventRegistrationCleanupPort eventPort;
    @Mock private FileStorageService fileStorageService;
    @InjectMocks private AccountWithdrawalService service;

    @Test
    void withdrawAnonymizesMembershipAndInvalidatesLogin() {
        User user = activeUser();
        Membership membership = member(MembershipRole.MEMBER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(membershipRepository.findAllIncludingInactiveByUserId(1L)).willReturn(List.of(membership));

        service.withdraw(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getName()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getStudentNumber()).isNull();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.LEFT);
        assertThat(membership.getUserId()).isNull();
        assertThat(membership.getMemberName()).isEqualTo("탈퇴 회원");
        assertThat(membership.getStudentNumber()).isEqualTo("W20");
        verify(notificationRepository).deleteAllByUserId(1L);
        verify(eventPort).cancelFutureRegistrations(org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(Instant.class));
        verify(authSessionRepository).deleteAllByUserId(1L);
        verify(oauthAccountRepository).deleteAllByUserId(1L);
        verify(userRepository).flush();
    }

    @Test
    void activeOwnerMustDelegateBeforeWithdrawal() {
        User user = activeUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(membershipRepository.findAllIncludingInactiveByUserId(1L)).willReturn(List.of(member(MembershipRole.OWNER)));

        assertThatThrownBy(() -> service.withdraw(1L))
                .isInstanceOfSatisfying(GeneralException.class,
                        error -> assertThat(error.getErrorCode().getCode()).isEqualTo("ORG_400_002"));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(authSessionRepository, never()).deleteAllByUserId(1L);
    }

    @Test
    void pendingOnboardingAccountCanWithdraw() {
        User user = User.pendingOnboarding();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        service.withdraw(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verify(oauthAccountRepository).deleteAllByUserId(1L);
    }

    private User activeUser() {
        User user = User.pendingOnboarding();
        user.completeOnboarding("김동방", "20260001", "컴퓨터공학과", "dongbang@example.com", Instant.now());
        return user;
    }

    private Membership member(MembershipRole role) {
        return Membership.builder()
                .id(20L)
                .organization(Organization.builder().id(10L).name("동방").slug("dongbang").build())
                .userId(1L)
                .memberName("김동방")
                .studentNumber("20260001")
                .role(role)
                .build();
    }
}
