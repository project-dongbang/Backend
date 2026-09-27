package com.dongbang.attendance.application;

import com.dongbang.attendance.application.dto.ModifyAttendanceCommand;
import com.dongbang.attendance.domain.AttendanceStatus;
import com.dongbang.attendance.exception.AttendanceErrorCode;
import com.dongbang.event.application.EventParticipationService;
import com.dongbang.event.application.command.ChangeParticipantsCommand;
import com.dongbang.event.application.facade.EventAttendanceFacade;
import com.dongbang.event.domain.ParticipantAction;
import com.dongbang.global.exception.GeneralException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.datasource.url=${ATTENDANCE_TEST_DB_URL:jdbc:postgresql://localhost:55459/attendance_test}",
        "spring.datasource.username=${ATTENDANCE_TEST_DB_USERNAME:attendance_test}",
        "spring.datasource.password=${ATTENDANCE_TEST_DB_PASSWORD:isolated_test_only}"
})
class AttendanceConcurrencyIntegrationTest {
    private static final Instant OPENED_AT = Instant.parse("2026-09-27T09:00:00Z");
    @Autowired AttendanceService service;
    @Autowired EventParticipationService participants;
    @Autowired EventAttendanceFacade events;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean Clock clock;

    private final AtomicReference<Instant> time = new AtomicReference<>();
    private ExecutorService executor;
    private Long organizationId;
    private Long userId;
    private Long membershipId;
    private Long eventId;
    private Long recordId;
    private String token;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        time.set(OPENED_AT);
        given(clock.instant()).willAnswer(invocation -> time.get());
        userId = jdbc.queryForObject("INSERT INTO users (status) VALUES ('ACTIVE') RETURNING user_id", Long.class);
        organizationId = jdbc.queryForObject("""
                INSERT INTO organizations (name, slug) VALUES ('출석 동시성 테스트', ?)
                RETURNING organization_id
                """, Long.class, UUID.randomUUID().toString());
        membershipId = jdbc.queryForObject("""
                INSERT INTO memberships (organization_id, user_id, member_name, student_number, generation, role)
                VALUES (?, ?, '테스트 회원', '20260001', '1', 'OWNER') RETURNING membership_id
                """, Long.class, organizationId, userId);
        eventId = jdbc.queryForObject("""
                INSERT INTO events (organization_id, created_by_membership_id, title, starts_at, ends_at)
                VALUES (?, ?, '테스트 행사', '2026-09-28T09:00:00Z', '2026-09-28T10:00:00Z') RETURNING event_id
                """, Long.class, organizationId, membershipId);
        jdbc.update("INSERT INTO event_registrations (event_id, membership_id, registered_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                eventId, membershipId);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        if (organizationId == null) return;
        jdbc.update("DELETE FROM notifications WHERE organization_id = ?", organizationId);
        jdbc.update("DELETE FROM audit_logs WHERE organization_id = ?", organizationId);
        jdbc.update("DELETE FROM attendance_records WHERE membership_id = ?", membershipId);
        jdbc.update("DELETE FROM attendance_sessions WHERE event_id = ?", eventId);
        jdbc.update("DELETE FROM event_registrations WHERE event_id = ?", eventId);
        jdbc.update("DELETE FROM events WHERE event_id = ?", eventId);
        jdbc.update("DELETE FROM memberships WHERE organization_id = ?", organizationId);
        jdbc.update("DELETE FROM organizations WHERE organization_id = ?", organizationId);
        jdbc.update("DELETE FROM users WHERE user_id = ?", userId);
    }

    @Test
    void concurrentStartsCreateOnlyOneSession() throws Exception {
        assertThat(runWaiting(this::startSession, this::startSession, () -> {}))
                .isEqualTo(AttendanceErrorCode.ALREADY_GENERATED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance_sessions WHERE event_id = ?", Long.class, eventId))
                .isEqualTo(1);
    }

    @Test
    void duplicateCheckInsHaveOneSuccess() throws Exception {
        startSession();
        assertThat(runWaiting(this::checkIn, this::checkIn, () -> {}))
                .isEqualTo(AttendanceErrorCode.ALREADY_PRESENT);
        assertThat(recordVersion()).isEqualTo(1);
    }

    @Test
    void checkInWaitingForCloseIsRejected() throws Exception {
        startSession();
        assertThat(runWaiting(() -> service.close(organizationId, userId, eventId), this::checkIn, () -> {}))
                .isEqualTo(AttendanceErrorCode.SESSION_CLOSED);
        assertThat(recordVersion()).isZero();
    }

    @Test
    void checkInWaitingForParticipantRemovalIsRejected() throws Exception {
        startSession();
        var command = new ChangeParticipantsCommand(0, List.of(
                new ChangeParticipantsCommand.Change(membershipId, ParticipantAction.REMOVE)));
        assertThat(runWaiting(() -> participants.changeParticipants(organizationId, userId, eventId, command),
                this::checkIn, () -> {})).isEqualTo(AttendanceErrorCode.PARTICIPANT_ONLY);
        assertThat(jdbc.queryForObject("SELECT target_active FROM attendance_records WHERE attendance_record_id = ?",
                Boolean.class, recordId)).isFalse();
    }

    @Test
    void manualEditWaitingForCheckInRejectsStaleVersion() throws Exception {
        startSession();
        assertThat(runWaiting(this::checkIn, () -> service.modify(organizationId, userId, eventId, recordId,
                new ModifyAttendanceCommand(AttendanceStatus.ABSENT, "출석 정정", 0)), () -> {}))
                .isEqualTo(AttendanceErrorCode.VERSION_CONFLICT);
        assertThat(jdbc.queryForObject("SELECT source FROM attendance_records WHERE attendance_record_id = ?",
                String.class, recordId)).isEqualTo("QR");
    }

    @Test
    void concurrentManualEditsRejectStaleVersion() throws Exception {
        startSession();
        Runnable edit = () -> service.modify(organizationId, userId, eventId, recordId,
                new ModifyAttendanceCommand(AttendanceStatus.PRESENT, "현장 확인", 0));
        assertThat(runWaiting(edit, edit, () -> {})).isEqualTo(AttendanceErrorCode.VERSION_CONFLICT);
        assertThat(recordVersion()).isEqualTo(1);
    }

    @Test
    void manualEditWaitingForRemovalCannotChangeInactiveRecord() throws Exception {
        startSession();
        var command = new ChangeParticipantsCommand(0, List.of(
                new ChangeParticipantsCommand.Change(membershipId, ParticipantAction.REMOVE)));
        assertThat(runWaiting(() -> participants.changeParticipants(organizationId, userId, eventId, command),
                () -> service.modify(organizationId, userId, eventId, recordId,
                        new ModifyAttendanceCommand(AttendanceStatus.PRESENT, "현장 확인", 0)), () -> {}))
                .isEqualTo(AttendanceErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void checkInUsesTimeAfterLockWait() throws Exception {
        startSession();
        assertThat(runWaiting(() -> events.lockEvent(organizationId, eventId), this::checkIn,
                () -> time.set(OPENED_AT.plusSeconds(600))))
                .isEqualTo(AttendanceErrorCode.QR_EXPIRED);
        assertThat(recordVersion()).isZero();
    }

    @Test
    void inactiveAttendanceRecordCannotCheckIn() {
        startSession();
        jdbc.update("UPDATE attendance_records SET target_active = false WHERE attendance_record_id = ?", recordId);
        assertThat(outcome(this::checkIn)).isEqualTo(AttendanceErrorCode.PARTICIPANT_ONLY);
    }

    @Test
    void checkInUsesTimeAfterRecordLockWait() throws Exception {
        startSession();
        assertThat(runWaiting(() -> jdbc.queryForObject(
                "SELECT attendance_record_id FROM attendance_records WHERE attendance_record_id = ? FOR UPDATE",
                Long.class, recordId), this::checkIn, () -> time.set(OPENED_AT.plusSeconds(600))))
                .isEqualTo(AttendanceErrorCode.QR_EXPIRED);
        assertThat(recordVersion()).isZero();
    }

    @Test
    void closeWaitingForCheckInPreservesAttendance() throws Exception {
        startSession();
        assertThat(runWaiting(this::checkIn, () -> service.close(organizationId, userId, eventId), () -> {}))
                .isEqualTo("SUCCESS");
        assertThat(recordVersion()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM attendance_sessions WHERE event_id = ?", String.class, eventId))
                .isEqualTo("CLOSED");
    }

    @Test
    void manualEditAfterExpirationRemainsAllowed() {
        startSession();
        time.set(OPENED_AT.plusSeconds(600));
        service.modify(organizationId, userId, eventId, recordId,
                new ModifyAttendanceCommand(AttendanceStatus.PRESENT, "현장 출석 확인", 0));
        assertThat(recordVersion()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT source FROM attendance_records WHERE attendance_record_id = ?",
                String.class, recordId)).isEqualTo("MANUAL");
    }

    private void startSession() {
        service.start(organizationId, userId, eventId);
        token = jdbc.queryForObject("SELECT qr_token FROM attendance_sessions WHERE event_id = ?", String.class, eventId);
        recordId = jdbc.queryForObject("SELECT attendance_record_id FROM attendance_records WHERE membership_id = ?",
                Long.class, membershipId);
    }

    private void checkIn() { service.checkIn(organizationId, userId, eventId, token); }

    private Long recordVersion() {
        return jdbc.queryForObject("SELECT version FROM attendance_records WHERE attendance_record_id = ?", Long.class, recordId);
    }

    private Object runWaiting(Runnable first, Runnable second, Runnable whileBlocked) throws Exception {
        Future<Object> future = new TransactionTemplate(transactionManager).execute(status -> {
            first.run();
            Future<Object> waiting = executor.submit(() -> outcome(second));
            // PostgreSQL 잠금 대기 확인 후 선행 트랜잭션 커밋
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean blocked = false;
            while (System.nanoTime() < deadline && !waiting.isDone()) {
                blocked = Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                        WHERE datname = current_database() AND cardinality(pg_blocking_pids(pid)) > 0)
                        """, Boolean.class));
                if (blocked) break;
                try { Thread.sleep(20); }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
            assertThat(blocked).as("후행 요청의 행사 잠금 대기").isTrue();
            whileBlocked.run();
            return waiting;
        });
        return future.get(10, TimeUnit.SECONDS);
    }

    private Object outcome(Runnable action) {
        try { action.run(); return "SUCCESS"; }
        catch (GeneralException exception) { return exception.getErrorCode(); }
    }
}
