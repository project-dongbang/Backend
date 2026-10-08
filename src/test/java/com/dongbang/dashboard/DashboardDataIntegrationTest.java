package com.dongbang.dashboard;

import com.dongbang.dashboard.application.DashboardQueryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@Transactional
class DashboardDataIntegrationTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired JdbcTemplate jdbc;
    @Autowired DashboardQueryService dashboards;

    @Test
    void dashboardUsesCurrentEventAttendanceAndFeeData() {
        Long adminUser = jdbc.queryForObject("INSERT INTO users (status) VALUES ('ACTIVE') RETURNING user_id", Long.class);
        Long memberUser = jdbc.queryForObject("INSERT INTO users (status) VALUES ('ACTIVE') RETURNING user_id", Long.class);
        Long organization = jdbc.queryForObject(
                "INSERT INTO organizations (name, slug) VALUES ('대시보드 테스트', ?) RETURNING organization_id",
                Long.class, UUID.randomUUID().toString());
        Long admin = membership(organization, adminUser, "ADMIN", "1");
        Long member = membership(organization, memberUser, "MEMBER", "2");

        Instant now = Instant.now();
        Instant monthStart = LocalDate.now(SEOUL).withDayOfMonth(1).atStartOfDay(SEOUL).toInstant();
        Long event = event(organization, admin, "EVENT", "이번 달 행사", monthStart.plusSeconds(3600), "SCHEDULED", false);
        event(organization, admin, "EVENT", "취소 행사", monthStart.plusSeconds(7200), "CANCELED", false);
        event(organization, admin, "SCHEDULE", "삭제 일정", now.plusSeconds(7200), "SCHEDULED", true);
        event(organization, admin, "SCHEDULE", "예정 일정", now.plusSeconds(3600), "SCHEDULED", false);

        Long session = jdbc.queryForObject("""
                INSERT INTO attendance_sessions (event_id, opened_by_membership_id, qr_token, opened_at)
                VALUES (?, ?, ?, ?) RETURNING attendance_session_id
                """, Long.class, event, admin, UUID.randomUUID().toString(), Timestamp.from(now.minusSeconds(900)));
        jdbc.update("""
                INSERT INTO attendance_records (attendance_session_id, membership_id, status, target_active)
                VALUES (?, ?, 'PRESENT', true), (?, ?, 'ABSENT', true)
                """, session, admin, session, member);

        Long feeItem = jdbc.queryForObject("""
                INSERT INTO fee_items (organization_id, title, due_date, bank_name, bank_account_number,
                                       account_holder, created_by_membership_id)
                VALUES (?, '회비', ?, '은행', '123', '동방', ?) RETURNING fee_item_id
                """, Long.class, organization, LocalDate.now(SEOUL), admin);
        Long category = jdbc.queryForObject("""
                INSERT INTO fee_categories (fee_item_id, name, amount, display_order)
                VALUES (?, '기본', 10000, 0) RETURNING fee_category_id
                """, Long.class, feeItem);
        jdbc.update("""
                INSERT INTO fee_targets (fee_item_id, fee_category_id, membership_id, amount_due, status)
                VALUES (?, ?, ?, 10000, 'PAID'), (?, ?, ?, 10000, 'UNPAID')
                """, feeItem, category, admin, feeItem, category, member);

        var adminDashboard = dashboards.getAdminDashboard(organization, adminUser);
        assertThat(adminDashboard.stats().thisMonthEventCount()).isEqualTo(1);
        assertThat(adminDashboard.stats().attendanceRate()).isEqualTo(50.0);
        assertThat(adminDashboard.stats().paymentRate()).isEqualTo(50.0);
        assertThat(adminDashboard.upcomingSchedules()).extracting("title")
                .contains("예정 일정").doesNotContain("취소 행사", "삭제 일정");

        var adminAsMember = dashboards.getMemberDashboard(organization, adminUser);
        assertThat(adminAsMember.myStats().attendanceRate()).isEqualTo(100.0);
        assertThat(adminAsMember.myStats().attendedEventCount()).isEqualTo(1);
        assertThat(adminAsMember.myStats().unpaidFeeCount()).isZero();

        var memberDashboard = dashboards.getMemberDashboard(organization, memberUser);
        assertThat(memberDashboard.myStats().attendanceRate()).isEqualTo(0.0);
        assertThat(memberDashboard.myStats().attendedEventCount()).isZero();
        assertThat(memberDashboard.myStats().unpaidFeeCount()).isEqualTo(1);
        assertThat(memberDashboard.stats().attendanceRate()).isEqualTo(50.0);
    }

    @Test
    void dashboardReturnsZeroForEmptyData() {
        Long user = jdbc.queryForObject("INSERT INTO users (status) VALUES ('ACTIVE') RETURNING user_id", Long.class);
        Long organization = jdbc.queryForObject(
                "INSERT INTO organizations (name, slug) VALUES ('빈 대시보드', ?) RETURNING organization_id",
                Long.class, UUID.randomUUID().toString());
        membership(organization, user, "ADMIN", "3");

        var dashboard = dashboards.getMemberDashboard(organization, user);
        assertThat(dashboard.stats().attendanceRate()).isZero();
        assertThat(dashboard.stats().paymentRate()).isZero();
        assertThat(dashboard.myStats().attendanceRate()).isZero();
        assertThat(dashboard.myStats().attendedEventCount()).isZero();
        assertThat(dashboard.myStats().unpaidFeeCount()).isZero();
        assertThat(dashboard.upcomingSchedules()).isEmpty();
    }

    private Long membership(Long organization, Long user, String role, String suffix) {
        return jdbc.queryForObject("""
                INSERT INTO memberships (organization_id, user_id, member_name, student_number, role)
                VALUES (?, ?, '회원', ?, ?) RETURNING membership_id
                """, Long.class, organization, user, UUID.randomUUID().toString().substring(0, 8) + suffix, role);
    }

    private Long event(Long organization, Long creator, String type, String title, Instant startsAt,
                       String status, boolean deleted) {
        return jdbc.queryForObject("""
                INSERT INTO events (organization_id, created_by_membership_id, event_type, title, location,
                                    starts_at, ends_at, status, deleted_at)
                VALUES (?, ?, ?, ?, '동아리방', ?, ?, ?, ?) RETURNING event_id
                """, Long.class, organization, creator, type, title, Timestamp.from(startsAt),
                Timestamp.from(startsAt.plusSeconds(3600)), status,
                deleted ? Timestamp.from(Instant.now()) : null);
    }
}
