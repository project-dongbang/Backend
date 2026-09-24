package com.dongbang.event.application;

import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventDetails;
import com.dongbang.event.domain.EventParticipant;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.ParticipantAction;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.ParticipantMemberSummary;
import com.dongbang.user.application.facade.UserAccountFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantCandidateServiceTest {
    @Mock EventAccessService access;
    @Mock EventRepository events;
    @Mock EventParticipantRepository participants;
    @Mock MembershipAccessFacade memberships;
    @Mock UserAccountFacade users;
    @InjectMocks ParticipantCandidateService service;

    @Test
    void searchKeepsGlobalCountAndIncludesInactiveParticipant() {
        Instant now = Instant.parse("2026-09-24T00:00:00Z");
        Event event = Event.builder().id(10L).organizationId(1L).type(EventType.EVENT)
                .details(new EventDetails("행사", null, "장소", now, now.plusSeconds(60), 20, null)).build();
        when(events.findForUpdate(10L, 1L)).thenReturn(Optional.of(event));
        when(participants.findByEventIdOrderByRegisteredAtAscIdAsc(10L))
                .thenReturn(List.of(new EventParticipant(10L, 2L, now)));
        when(memberships.getParticipantMembers(1L)).thenReturn(List.of(
                new ParticipantMemberSummary(2L, null, "기존 회원", "20260001", false),
                new ParticipantMemberSummary(3L, 5L, "신규 회원", "20260002", true),
                new ParticipantMemberSummary(4L, null, "탈퇴 회원", "20260003", false)));
        when(users.getDepartments(List.of(5L))).thenReturn(Map.of(5L, "컴퓨터공학과"));

        var result = service.candidates(1L, 7L, 10L, "2026", 1, 1);
        assertThat(result.participantCount()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.members()).singleElement().satisfies(member -> {
            assertThat(member.membershipId()).isEqualTo(3L);
            assertThat(member.department()).isEqualTo("컴퓨터공학과");
            assertThat(member.allowedActions()).containsExactly(ParticipantAction.ADD);
        });
        verify(access).requireStaff(1L, 7L);
    }
}
