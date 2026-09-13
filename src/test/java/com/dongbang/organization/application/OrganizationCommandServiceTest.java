package com.dongbang.organization.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.domain.*;
import com.dongbang.organization.domain.repository.InvitationRepository;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.presentation.dto.request.*;
import com.dongbang.organization.presentation.dto.response.CreateOrganizationResponse;
import com.dongbang.organization.presentation.dto.response.JoinOrganizationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
