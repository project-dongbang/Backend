package com.dongbang.mypage.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.mypage.application.port.MyActivityEventPort;
import com.dongbang.mypage.application.port.MyActivityFeePort;
import com.dongbang.mypage.application.port.RegisteredEventActivity;
import com.dongbang.mypage.domain.MyFeeStatus;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyPageActivityServiceTest {
    @Mock OrganizationRepository organizationRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock MyActivityEventPort eventPort;
    @Mock MyActivityFeePort feePort;

    private MyPageActivityService service;
    private Organization organization;
    private Membership membership;

    @BeforeEach
    void setUp() {
        service = new MyPageActivityService(organizationRepository, membershipRepository, eventPort, feePort);
        organization = Organization.builder().id(10L).name("동방").slug("dongbang").build();
        membership = Membership.builder().id(20L).organization(organization).userId(1L)
                .memberName("김동방").studentNumber("20260001").role(MembershipRole.MEMBER).build();
    }

    @Test
    void returnsSummaryAndRegisteredEvents() {
        given(organizationRepository.findById(10L)).willReturn(Optional.of(organization));
        given(membershipRepository.findByOrganizationIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(eventPort.findRegisteredEvents(10L, 20L)).willReturn(List.of(
                new RegisteredEventActivity(31L, "프론트 스터디", Instant.parse("2026-09-17T10:00:00Z"))));
        given(feePort.getFeeStatus(20L)).willReturn(MyFeeStatus.UNPAID);

        var response = service.getMyActivities(1L, 10L);

        assertThat(response.summary().eventRegistrationCount()).isEqualTo(1);
        assertThat(response.summary().feeStatus()).isEqualTo(MyFeeStatus.UNPAID);
        assertThat(response.registeredEvents()).singleElement().satisfies(event -> {
            assertThat(event.eventId()).isEqualTo(31L);
            assertThat(event.registrationStatus()).isEqualTo("REGISTERED");
        });
    }

    @Test
    void rejectsUserOutsideOrganization() {
        given(organizationRepository.findById(10L)).willReturn(Optional.of(organization));
        given(membershipRepository.findByOrganizationIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyActivities(1L, 10L))
                .isInstanceOfSatisfying(GeneralException.class,
                        error -> assertThat(error.getErrorCode().getCode()).isEqualTo("AUTH_403_001"));
    }
}
