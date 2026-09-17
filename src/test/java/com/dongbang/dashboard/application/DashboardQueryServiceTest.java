package com.dongbang.dashboard.application;

import com.dongbang.dashboard.application.port.DashboardAttendancePort;
import com.dongbang.dashboard.application.port.DashboardEventPort;
import com.dongbang.dashboard.application.port.DashboardFinancePort;
import com.dongbang.dashboard.application.port.DashboardScheduleItem;
import com.dongbang.dashboard.exception.DashboardErrorCode;
import com.dongbang.dashboard.presentation.dto.AdminDashboardResponse;
import com.dongbang.dashboard.presentation.dto.MemberDashboardResponse;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.photo.application.facade.PhotoAccessFacade;
import com.dongbang.photo.application.facade.PhotoSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MembershipAccessFacade membershipAccessFacade;

    @Mock
    private PhotoAccessFacade photoAccessFacade;

    @Mock
    private DashboardEventPort eventPort;

    @Mock
    private DashboardAttendancePort attendancePort;

    @Mock
    private DashboardFinancePort financePort;

    @InjectMocks
    private DashboardQueryService dashboardQueryService;

    private final Long orgId = 1L;
    private final Long userId = 10L;

    @Nested
    @DisplayName("운영진 대시보드 조회")
    class AdminDashboardTest {

        @Test
        @DisplayName("성공: 운영진이 동아리 전체 요약 통계와 일정, 최근 사진을 조회한다")
        void getAdminDashboard_success() {
            // given
            Organization org = Organization.builder().id(orgId).name("동방").slug("dongbang").build();
            DashboardScheduleItem schedule = new DashboardScheduleItem(
                    1L, "01", 9, "개강 총회", "19:00 · 학생회관", "행사", Instant.now()
            );
            PhotoSummary photo = new PhotoSummary(10L, "MT 단체사진", "/uploads/photo.jpg");

            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(true);
            given(organizationRepository.findById(orgId)).willReturn(Optional.of(org));
            given(membershipAccessFacade.getActiveMemberCount(orgId)).willReturn(64L);
            given(eventPort.countThisMonthEvents(orgId)).willReturn(8);
            given(attendancePort.getOrganizationAttendanceRate(orgId)).willReturn(82.0);
            given(financePort.getFeePaymentRate(orgId)).willReturn(91.0);
            given(eventPort.getUpcomingSchedules(orgId, 5)).willReturn(List.of(schedule));
            given(photoAccessFacade.getRecentPhotos(orgId, 4)).willReturn(List.of(photo));

            // when
            AdminDashboardResponse response = dashboardQueryService.getAdminDashboard(orgId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.organizationId()).isEqualTo(orgId);
            assertThat(response.organizationName()).isEqualTo("동방");
            assertThat(response.stats().activeMemberCount()).isEqualTo(64L);
            assertThat(response.stats().thisMonthEventCount()).isEqualTo(8);
            assertThat(response.stats().attendanceRate()).isEqualTo(82.0);
            assertThat(response.stats().paymentRate()).isEqualTo(91.0);
            assertThat(response.upcomingSchedules()).hasSize(1);
            assertThat(response.upcomingSchedules().get(0).title()).isEqualTo("개강 총회");
            assertThat(response.recentPhotos()).hasSize(1);
            assertThat(response.recentPhotos().get(0).title()).isEqualTo("MT 단체사진");
        }

        @Test
        @DisplayName("실패: 일반 회원이 운영진 대시보드 요청 시 STAFF_REQUIRED 예외 발생")
        void getAdminDashboard_notStaff_throwsException() {
            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(false);

            assertThatThrownBy(() -> dashboardQueryService.getAdminDashboard(orgId, userId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DashboardErrorCode.STAFF_REQUIRED);
        }

        @Test
        @DisplayName("실패: 동아리가 존재하지 않으면 ORGANIZATION_NOT_FOUND 예외 발생")
        void getAdminDashboard_orgNotFound_throwsException() {
            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(true);
            given(organizationRepository.findById(orgId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> dashboardQueryService.getAdminDashboard(orgId, userId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DashboardErrorCode.ORGANIZATION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("일반 회원 대시보드 조회")
    class MemberDashboardTest {

        @Test
        @DisplayName("성공: 회원이 내 개인 통계와 동아리 요약 통계를 조회한다")
        void getMemberDashboard_success() {
            // given
            Organization org = Organization.builder().id(orgId).name("동방").slug("dongbang").build();
            DashboardScheduleItem schedule = new DashboardScheduleItem(
                    1L, "05", 9, "신입 부원 환영", "16:00 · 라운지", "행사", Instant.now()
            );

            given(membershipAccessFacade.isActiveMember(orgId, userId)).willReturn(true);
            given(organizationRepository.findById(orgId)).willReturn(Optional.of(org));
            given(attendancePort.getMemberAttendanceRate(orgId, userId)).willReturn(85.0);
            given(attendancePort.getMemberAttendanceCount(orgId, userId)).willReturn(6);
            given(financePort.getMemberUnpaidFeeCount(orgId, userId)).willReturn(0);
            given(membershipAccessFacade.getActiveMemberCount(orgId)).willReturn(64L);
            given(eventPort.countThisMonthEvents(orgId)).willReturn(8);
            given(attendancePort.getOrganizationAttendanceRate(orgId)).willReturn(82.0);
            given(financePort.getFeePaymentRate(orgId)).willReturn(91.0);
            given(eventPort.getUpcomingSchedules(orgId, 5)).willReturn(List.of(schedule));
            given(photoAccessFacade.getRecentPhotos(orgId, 4)).willReturn(List.of());

            // when
            MemberDashboardResponse response = dashboardQueryService.getMemberDashboard(orgId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.myStats().attendanceRate()).isEqualTo(85.0);
            assertThat(response.myStats().attendedEventCount()).isEqualTo(6);
            assertThat(response.myStats().unpaidFeeCount()).isEqualTo(0);
            assertThat(response.stats().activeMemberCount()).isEqualTo(64L);
            assertThat(response.upcomingSchedules()).hasSize(1);
            assertThat(response.recentPhotos()).isEmpty();
        }

        @Test
        @DisplayName("실패: 비회원이 회원 대시보드 요청 시 MEMBER_REQUIRED 예외 발생")
        void getMemberDashboard_notMember_throwsException() {
            given(membershipAccessFacade.isActiveMember(orgId, userId)).willReturn(false);

            assertThatThrownBy(() -> dashboardQueryService.getMemberDashboard(orgId, userId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DashboardErrorCode.MEMBER_REQUIRED);
        }
    }
}
