package com.dongbang.attendance.application;

import com.dongbang.attendance.application.event.AttendanceStartedEvent;
import com.dongbang.attendance.application.port.QrTokenGenerator;
import com.dongbang.attendance.domain.AttendanceRecordRepository;
import com.dongbang.attendance.domain.AttendanceSession;
import com.dongbang.attendance.domain.AttendanceSessionRepository;
import com.dongbang.event.application.EventAccessService;
import com.dongbang.event.application.facade.EventAttendanceFacade;
import com.dongbang.finance.application.facade.AuditLogFacade;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceEventTest {
    @Mock AttendanceSessionRepository sessions;
    @Mock AttendanceRecordRepository records;
    @Mock EventAttendanceFacade events;
    @Mock EventAccessService access;
    @Mock MembershipAccessFacade memberships;
    @Mock QrTokenGenerator qrTokens;
    @Mock AuditLogFacade auditLog;
    @Mock ApplicationEventPublisher eventPublisher;

    @Test
    void startPublishesEventForRegisteredParticipants() {
        Instant now = Instant.parse("2026-09-24T09:00:00Z");
        AttendanceService service = new AttendanceService(
                sessions, records, events, access, memberships, qrTokens, auditLog,
                Clock.fixed(now, ZoneOffset.UTC), eventPublisher);
        given(access.requireStaff(1L, 2L)).willReturn(9L);
        given(events.lockEvent(1L, 31L)).willReturn(new EventAttendanceFacade.AttendanceEvent(
                31L, 1L, "개강 총회", now.plusSeconds(3600), now.plusSeconds(7200), "동아리방"));
        given(sessions.findByEventId(31L)).willReturn(Optional.empty());
        given(qrTokens.generate()).willReturn("qr-token");
        given(sessions.save(any())).willAnswer(invocation -> {
            AttendanceSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 41L);
            return session;
        });
        given(events.participantIds(31L)).willReturn(List.of(11L, 12L));

        service.start(1L, 2L, 31L);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue())
                .isInstanceOfSatisfying(AttendanceStartedEvent.class, started -> {
                    org.assertj.core.api.Assertions.assertThat(started.attendanceSessionId()).isEqualTo(41L);
                    org.assertj.core.api.Assertions.assertThat(started.participantMembershipIds())
                            .containsExactly(11L, 12L);
                });
    }
}
