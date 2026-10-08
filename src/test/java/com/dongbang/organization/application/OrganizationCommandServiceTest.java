package com.dongbang.organization.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.domain.*;
import com.dongbang.organization.domain.repository.InvitationRepository;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.presentation.dto.request.*;
import com.dongbang.organization.presentation.dto.OrganizationPaymentAccount;
import com.dongbang.organization.presentation.dto.response.CreateOrganizationResponse;
import com.dongbang.organization.presentation.dto.response.JoinOrganizationResponse;
import com.dongbang.auth.infrastructure.token.TokenHashService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrganizationCommandServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private com.dongbang.user.application.facade.UserAccountFacade userAccountFacade;

    @Mock
    private TokenHashService tokenHashService;

    @InjectMocks
    private OrganizationCommandService organizationCommandService;

    @Nested
    @DisplayName("동아리 생성")
    class CreateOrganizationTest {

        @Test
        @DisplayName("성공: 슬러그가 중복되지 않으면 동아리와 OWNER 멤버십이 생성된다")
        void success() {
            // given
            Long userId = 1L;
            CreateOrganizationRequest request = new CreateOrganizationRequest(
                    "동방 개발팀", "dongbang-dev", "동아리 설명", "https://logo.png"
            );

            Organization savedOrg = Organization.builder()
                    .name(request.name())
                    .slug(request.slug())
                    .description(request.description())
                    .logoUrl(request.logoUrl())
                    .build();

            given(organizationRepository.existsBySlug("dongbang-dev")).willReturn(false);
            given(organizationRepository.save(any(Organization.class))).willReturn(savedOrg);
            given(userAccountFacade.getAccount(userId)).willReturn(
                    new com.dongbang.user.application.facade.UserAccountSummary(
                            userId, "홍길동", "20240001", "컴퓨터공학과", "test@dongbang.com",
                            com.dongbang.user.domain.UserStatus.ACTIVE, Instant.now()
                    )
            );

            // when
            CreateOrganizationResponse response = organizationCommandService.createOrganization(userId, request);

            // then
            assertThat(response.slug()).isEqualTo("dongbang-dev");
            verify(membershipRepository).save(any(Membership.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 슬러그인 경우 SLUG_ALREADY_EXISTS 예외가 발생한다")
        void fail_slug_duplicate() {
            // given
            CreateOrganizationRequest request = new CreateOrganizationRequest(
                    "동방 개발팀", "dongbang-dev", null, null
            );
            given(organizationRepository.existsBySlug("dongbang-dev")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> organizationCommandService.createOrganization(1L, request))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", OrganizationErrorCode.SLUG_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("동아리 운영 설정")
    class UpdateSettingsTest {
        private final Long orgId = 10L;
        private final Long userId = 1L;

        @Test
        @DisplayName("대표는 학기·기본 회비·계좌를 설정한다")
        void ownerUpdatesSettings() {
            Organization organization = Organization.builder().id(orgId).name("동방").slug("dongbang").build();
            Membership owner = Membership.builder().organization(organization).userId(userId)
                    .role(MembershipRole.OWNER).memberName("대표").studentNumber("20240001").build();
            given(membershipRepository.findByOrganizationIdAndUserId(orgId, userId)).willReturn(Optional.of(owner));
            given(organizationRepository.findById(orgId)).willReturn(Optional.of(organization));
            UpdateOrganizationRequest request = new UpdateOrganizationRequest(null, null, null,
                    new UpdateOrganizationSettingsRequest("2026-2", new BigDecimal("40000"),
                            new OrganizationPaymentAccount("국민은행", "123-456", "동방")));

            organizationCommandService.updateOrganization(userId, orgId, request);

            assertThat(organization.getOperatingSemester()).isEqualTo("2026-2");
            assertThat(organization.getDefaultFeeAmount()).isEqualByComparingTo("40000");
            assertThat(organization.getFeeBankName()).isEqualTo("국민은행");
            assertThat(organization.getFeeAccountNumber()).isEqualTo("123-456");
        }

        @Test
        @DisplayName("운영진은 기존 기본 정보를 수정할 수 있지만 회비 설정은 수정할 수 없다")
        void staffCannotUpdateSettings() {
            Organization organization = Organization.builder().id(orgId).name("동방").slug("dongbang").build();
            Membership admin = Membership.builder().organization(organization).userId(userId)
                    .role(MembershipRole.ADMIN).memberName("운영진").studentNumber("20240001").build();
            given(membershipRepository.findByOrganizationIdAndUserId(orgId, userId)).willReturn(Optional.of(admin));
            given(organizationRepository.findById(orgId)).willReturn(Optional.of(organization));

            organizationCommandService.updateOrganization(userId, orgId,
                    new UpdateOrganizationRequest("새 이름", null, null, null));
            assertThat(organization.getName()).isEqualTo("새 이름");

            assertThatThrownBy(() -> organizationCommandService.updateOrganization(userId, orgId,
                    new UpdateOrganizationRequest(null, null, null,
                            new UpdateOrganizationSettingsRequest("2026-2", null, null))))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", OrganizationErrorCode.OWNER_REQUIRED);
            assertThat(organization.getOperatingSemester()).isNull();
        }
    }

    @Nested
    @DisplayName("멤버 운영 정보 수정")
    class UpdateMemberInfoTest {
        private final Long orgId = 10L;
        private final Long userId = 1L;
        private final Long memberId = 20L;

        private Organization organization() {
            return Organization.builder().id(orgId).name("동방").slug("dongbang").build();
        }

        private void givenEditorAndTarget(Membership editor, Membership target) {
            given(membershipRepository.findByOrganizationIdAndUserId(orgId, userId)).willReturn(Optional.of(editor));
            given(organizationRepository.findById(orgId)).willReturn(Optional.of(editor.getOrganization()));
            given(membershipRepository.findById(memberId)).willReturn(Optional.of(target));
        }

        @Test
        @DisplayName("운영진은 일반 회원을 비활성화하고 기수·직책을 수정한다")
        void staffUpdatesMember() {
            Organization org = organization();
            Membership editor = Membership.builder().organization(org).userId(userId).role(MembershipRole.ADMIN)
                    .memberName("운영진").studentNumber("20240001").build();
            Membership target = Membership.builder().id(memberId).organization(org).userId(2L)
                    .memberName("회원").studentNumber("20240002").build();
            givenEditorAndTarget(editor, target);

            var response = organizationCommandService.updateMemberInfo(userId, orgId, memberId,
                    new UpdateMemberInfoRequest(UpdateMemberInfoRequest.ActivityStatus.INACTIVE, " 13기 ", " 총무 "));

            assertThat(response.status()).isEqualTo(MembershipStatus.INACTIVE);
            assertThat(response.generation()).isEqualTo("13기");
            assertThat(response.position()).isEqualTo("총무");
        }

        @Test
        @DisplayName("대표는 본인을 비활성화할 수 없다")
        void ownerCannotDeactivate() {
            Organization org = organization();
            Membership owner = Membership.builder().id(memberId).organization(org).userId(userId)
                    .role(MembershipRole.OWNER).memberName("대표").studentNumber("20240001").build();
            givenEditorAndTarget(owner, owner);

            assertThatThrownBy(() -> organizationCommandService.updateMemberInfo(userId, orgId, memberId,
                    new UpdateMemberInfoRequest(UpdateMemberInfoRequest.ActivityStatus.INACTIVE, null, null)))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", OrganizationErrorCode.OWNER_CANNOT_DEACTIVATE);
            assertThat(owner.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        }

        @Test
        @DisplayName("탈퇴 회원은 운영 정보 수정으로 재가입시킬 수 없다")
        void leftMemberCannotBeReactivated() {
            Organization org = organization();
            Membership editor = Membership.builder().organization(org).userId(userId).role(MembershipRole.ADMIN)
                    .memberName("운영진").studentNumber("20240001").build();
            Membership target = Membership.builder().id(memberId).organization(org).userId(2L)
                    .memberName("회원").studentNumber("20240002").build();
            target.leave();
            givenEditorAndTarget(editor, target);

            assertThatThrownBy(() -> organizationCommandService.updateMemberInfo(userId, orgId, memberId,
                    new UpdateMemberInfoRequest(UpdateMemberInfoRequest.ActivityStatus.ACTIVE, null, null)))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", OrganizationErrorCode.MEMBER_STATUS_NOT_EDITABLE);
        }

    }

    @Nested
    @DisplayName("대표 권한 위임")
    class DelegateOwnerTest {

        @Test
        @DisplayName("성공: 회장이 ADMIN에게 권한을 위임하면 타겟은 OWNER가 되고 기존 회장은 ADMIN이 된다")
        void success() {
            // given
            Long ownerUserId = 1L;
            Long orgId = 10L;
            Long targetMemberId = 20L;

            Organization org = Organization.builder().id(orgId).name("동방").slug("dongbang").build();

            Membership currentOwner = Membership.builder()
                    .id(1L)
                    .organization(org)
                    .userId(ownerUserId)
                    .memberName("회장")
                    .studentNumber("20200001")
                    .role(MembershipRole.OWNER)
                    .build();

            Membership targetAdmin = Membership.builder()
                    .id(targetMemberId)
                    .organization(org)
                    .userId(2L)
                    .memberName("부회장")
                    .studentNumber("20200002")
                    .role(MembershipRole.ADMIN)
                    .build();

            given(membershipRepository.findByOrganizationIdAndUserId(orgId, ownerUserId))
                    .willReturn(Optional.of(currentOwner));
            given(membershipRepository.findById(targetMemberId))
                    .willReturn(Optional.of(targetAdmin));

            // when
            organizationCommandService.delegateOwner(ownerUserId, orgId, new DelegateOwnerRequest(targetMemberId));

            // then
            assertThat(targetAdmin.getRole()).isEqualTo(MembershipRole.OWNER);
            assertThat(currentOwner.getRole()).isEqualTo(MembershipRole.ADMIN);
        }

        @Test
        @DisplayName("실패: ADMIN이 아닌 일반 MEMBER에게 위임 시도 시 INVALID_DELEGATION_TARGET 예외 발생")
        void fail_not_admin() {
            // given
            Long ownerUserId = 1L;
            Long orgId = 10L;
            Long targetMemberId = 20L;

            Organization org = Organization.builder().id(orgId).name("동방").slug("dongbang").build();

            Membership currentOwner = Membership.builder()
                    .id(1L)
                    .organization(org)
                    .userId(ownerUserId)
                    .role(MembershipRole.OWNER)
                    .memberName("회장")
                    .studentNumber("20200001")
                    .build();

            Membership targetNormal = Membership.builder()
                    .id(targetMemberId)
                    .organization(org)
                    .userId(2L)
                    .role(MembershipRole.MEMBER)
                    .memberName("일반")
                    .studentNumber("20200002")
                    .build();

            given(membershipRepository.findByOrganizationIdAndUserId(orgId, ownerUserId))
                    .willReturn(Optional.of(currentOwner));
            given(membershipRepository.findById(targetMemberId))
                    .willReturn(Optional.of(targetNormal));

            // when & then
            assertThatThrownBy(() -> organizationCommandService.delegateOwner(ownerUserId, orgId, new DelegateOwnerRequest(targetMemberId)))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", OrganizationErrorCode.INVALID_DELEGATION_TARGET);
        }
    }

    @Nested
    @DisplayName("초대 수락 및 가입")
    class AcceptInvitationTest {

        @Test
        @DisplayName("성공: 유효한 초대 토큰으로 가입 시 회원의 실제 프로필 정보로 MEMBER가 생성된다")
        void success() {
            // given
            Long userId = 2L;
            String token = "valid-token";
            String tokenHash = "hashed-token";
            Organization org = Organization.builder().id(10L).name("동방").slug("dongbang").build();
            Invitation invitation = Invitation.builder()
                    .organization(org)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .build();

            given(tokenHashService.hash(token)).willReturn(tokenHash);
            given(invitationRepository.findByTokenHash(tokenHash)).willReturn(Optional.of(invitation));
            given(membershipRepository.findByOrganizationIdAndUserId(10L, userId)).willReturn(Optional.empty());
            given(userAccountFacade.getAccount(userId)).willReturn(
                    new com.dongbang.user.application.facade.UserAccountSummary(
                            userId, "이순신", "20240002", "기계공학과", "soonshin@dongbang.com",
                            com.dongbang.user.domain.UserStatus.ACTIVE, Instant.now()
                    )
            );
            given(membershipRepository.save(any(Membership.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            JoinOrganizationResponse response = organizationCommandService.acceptInvitation(userId, token);

            // then
            assertThat(response.organizationId()).isEqualTo(10L);
            assertThat(response.role()).isEqualTo(MembershipRole.MEMBER);
            verify(membershipRepository).save(any(Membership.class));
        }
    }

    @Nested
    @DisplayName("동아리 탈퇴")
    class LeaveOrganizationTest {

        @Test
        @DisplayName("실패: 회장(OWNER)은 권한을 위임하기 전까지 탈퇴할 수 없다")
        void fail_owner_cannot_leave() {
            // given
            Long ownerUserId = 1L;
            Long orgId = 10L;

            Membership owner = Membership.builder()
                    .role(MembershipRole.OWNER)
                    .memberName("회장")
                    .studentNumber("20200001")
                    .build();

            given(membershipRepository.findByOrganizationIdAndUserId(orgId, ownerUserId))
                    .willReturn(Optional.of(owner));

            // when & then
            assertThatThrownBy(() -> organizationCommandService.leaveOrganization(ownerUserId, orgId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", OrganizationErrorCode.OWNER_CANNOT_LEAVE);
        }
    }
}
