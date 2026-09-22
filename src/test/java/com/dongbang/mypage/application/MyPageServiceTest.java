package com.dongbang.mypage.application;

import com.dongbang.auth.domain.OAuthAccount;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.domain.repository.OAuthAccountRepository;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.mypage.presentation.dto.request.UpdateMyProfileRequest;
import com.dongbang.mypage.presentation.dto.response.MyProfileResponse;
import com.dongbang.mypage.presentation.dto.response.UpdateMyProfileResponse;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.user.domain.User;
import com.dongbang.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OAuthAccountRepository oauthAccountRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private MembershipRepository membershipRepository;

    @InjectMocks private MyPageService myPageService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.pendingOnboarding();
        user.completeOnboarding(
                "김동방",
                "20260004",
                "컴퓨터정보공학부",
                "dongbang@example.com",
                Instant.parse("2026-09-01T00:00:00Z")
        );
    }

    @Test
    @DisplayName("선택 동아리가 없으면 기본 프로필과 OAuth 제공자를 조회한다")
    void getMyProfileWithoutOrganization() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(oauthAccountRepository.findAllByUserId(1L)).willReturn(List.of(
                OAuthAccount.create(1L, OAuthProvider.KAKAO, "kakao-id", null),
                OAuthAccount.create(1L, OAuthProvider.GOOGLE, "google-id", null)
        ));

        MyProfileResponse response = myPageService.getMyProfile(1L, null);

        assertThat(response.name()).isEqualTo("김동방");
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.oauthProviders()).containsExactly(OAuthProvider.GOOGLE, OAuthProvider.KAKAO);
        assertThat(response.currentMembership()).isNull();
    }

    @Test
    @DisplayName("선택 동아리가 있으면 역할, 기수, 직책을 함께 조회한다")
    void getMyProfileWithOrganization() {
        Organization organization = Organization.builder().id(10L).name("동방").slug("dongbang").build();
        Membership membership = Membership.builder()
                .id(20L)
                .organization(organization)
                .userId(1L)
                .memberName("김동방")
                .studentNumber("20260004")
                .generation("11기")
                .position("총무")
                .role(MembershipRole.ADMIN)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(oauthAccountRepository.findAllByUserId(1L)).willReturn(List.of());
        given(organizationRepository.findById(10L)).willReturn(Optional.of(organization));
        given(membershipRepository.findByOrganizationIdAndUserId(10L, 1L))
                .willReturn(Optional.of(membership));

        MyProfileResponse response = myPageService.getMyProfile(1L, 10L);

        assertThat(response.currentMembership().organizationId()).isEqualTo(10L);
        assertThat(response.currentMembership().role()).isEqualTo("MANAGER");
        assertThat(response.currentMembership().generation()).isEqualTo("11기");
        assertThat(response.currentMembership().position()).isEqualTo("총무");
    }

    @Test
    @DisplayName("선택 동아리의 활성 회원이 아니면 접근을 거부한다")
    void getMyProfileRejectsNonMember() {
        Organization organization = Organization.builder().id(10L).name("동방").slug("dongbang").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(organizationRepository.findById(10L)).willReturn(Optional.of(organization));
        given(membershipRepository.findByOrganizationIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> myPageService.getMyProfile(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getErrorCode().getCode())
                        .isEqualTo("AUTH_403_001"));
    }

    @Test
    @DisplayName("전달된 프로필 필드를 정규화하여 수정한다")
    void updateMyProfile() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).willReturn(false);

        UpdateMyProfileResponse response = myPageService.updateMyProfile(
                1L,
                new UpdateMyProfileRequest(" 김동방 ", null, " 소프트웨어학과 ", " NEW@EXAMPLE.COM ")
        );

        assertThat(response.name()).isEqualTo("김동방");
        assertThat(response.department()).isEqualTo("소프트웨어학과");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.studentNumber()).isEqualTo("20260004");
        verify(userRepository).flush();
    }

    @Test
    @DisplayName("다른 사용자가 사용 중인 학번으로 수정할 수 없다")
    void updateMyProfileRejectsDuplicateStudentNumber() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByStudentNumberAndIdNot("20260005", 1L)).willReturn(true);

        assertThatThrownBy(() -> myPageService.updateMyProfile(
                1L,
                new UpdateMyProfileRequest(null, "20260005", null, null)
        ))
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getErrorCode().getCode())
                        .isEqualTo("USER_409_002"));
    }
}
