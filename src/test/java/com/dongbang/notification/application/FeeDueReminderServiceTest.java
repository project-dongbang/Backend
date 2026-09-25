package com.dongbang.notification.application;

import com.dongbang.finance.domain.FeeItem;
import com.dongbang.finance.domain.FeeTarget;
import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.repository.FeeItemRepository;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.MembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeeDueReminderServiceTest {

    @Mock private FeeItemRepository feeItemRepository;
    @Mock private FeeTargetRepository feeTargetRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private NotificationCreator notificationCreator;

    @Test
    void createsReminderOnlyForActiveLinkedUnpaidTargets() {
        LocalDate dueDate = LocalDate.of(2026, 9, 25);
        FeeItem feeItem = new FeeItem(1L, "2026년 2학기 정기 납부", dueDate, null,
                "카카오뱅크", "3333-12-3456789", "동방", 7L);
        ReflectionTestUtils.setField(feeItem, "id", 21L);
        FeeTarget linkedTarget = new FeeTarget(21L, 31L, 41L, BigDecimal.valueOf(40000));
        FeeTarget unlinkedTarget = new FeeTarget(21L, 31L, 42L, BigDecimal.valueOf(40000));

        Organization organization = Organization.builder().id(1L).name("동방").slug("dongbang").build();
        Membership linkedMember = Membership.builder()
                .id(41L).organization(organization).userId(10L).memberName("김동방")
                .studentNumber("20260001").role(MembershipRole.MEMBER).build();
        Membership unlinkedMember = Membership.builder()
                .id(42L).organization(organization).memberName("미연결")
                .studentNumber("20260002").role(MembershipRole.MEMBER).build();

        given(feeItemRepository.findAllByDueDate(dueDate)).willReturn(List.of(feeItem));
        given(feeTargetRepository.findAllByFeeItemIdInAndStatus(Set.of(21L), FeeTargetStatus.UNPAID))
                .willReturn(List.of(linkedTarget, unlinkedTarget));
        given(membershipRepository.findAllByIdIn(Set.of(41L, 42L)))
                .willReturn(List.of(linkedMember, unlinkedMember));
        given(notificationCreator.saveNew(org.mockito.ArgumentMatchers.anyList())).willReturn(1);

        FeeDueReminderService service = new FeeDueReminderService(
                feeItemRepository, feeTargetRepository, membershipRepository, notificationCreator);
        int createdCount = service.createRemindersFor(dueDate);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<Notification>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(notificationCreator).saveNew(captor.capture());
        assertThat(createdCount).isEqualTo(1);
        assertThat(captor.getValue()).singleElement().satisfies(notification -> {
            assertThat(notification.getUserId()).isEqualTo(10L);
            assertThat(notification.getType()).isEqualTo(NotificationType.FEE_DUE_REMINDER);
            assertThat(notification.getMessage()).contains("2026-09-25", "40,000원");
            assertThat(notification.getDeduplicationKey())
                    .isEqualTo("FEE_DUE_REMINDER:21:2026-09-25:10");
        });
    }

    @Test
    void doesNotCreateDuplicateReminder() {
        LocalDate dueDate = LocalDate.of(2026, 9, 25);
        FeeItem feeItem = new FeeItem(1L, "정기 납부", dueDate, null,
                "은행", "123", "동방", 7L);
        ReflectionTestUtils.setField(feeItem, "id", 21L);
        FeeTarget target = new FeeTarget(21L, 31L, 41L, BigDecimal.valueOf(10000));
        Organization organization = Organization.builder().id(1L).name("동방").slug("dongbang").build();
        Membership member = Membership.builder()
                .id(41L).organization(organization).userId(10L).memberName("김동방")
                .studentNumber("20260001").role(MembershipRole.MEMBER).build();
        given(feeItemRepository.findAllByDueDate(dueDate)).willReturn(List.of(feeItem));
        given(feeTargetRepository.findAllByFeeItemIdInAndStatus(Set.of(21L), FeeTargetStatus.UNPAID))
                .willReturn(List.of(target));
        given(membershipRepository.findAllByIdIn(Set.of(41L))).willReturn(List.of(member));
        given(notificationCreator.saveNew(org.mockito.ArgumentMatchers.anyList())).willReturn(0);

        FeeDueReminderService service = new FeeDueReminderService(
                feeItemRepository, feeTargetRepository, membershipRepository, notificationCreator);

        assertThat(service.createRemindersFor(dueDate)).isZero();
        verify(notificationCreator).saveNew(org.mockito.ArgumentMatchers.anyList());
    }
}
