package com.dongbang.notification.application;

import com.dongbang.finance.domain.FeeItem;
import com.dongbang.finance.domain.FeeTarget;
import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.repository.FeeItemRepository;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.NotificationReferenceType;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeDueReminderService {
    private static final String TITLE = "납부 마감 하루 전입니다.";

    private final FeeItemRepository feeItemRepository;
    private final FeeTargetRepository feeTargetRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationCreator notificationCreator;

    @Transactional
    public int createRemindersFor(LocalDate dueDate) {
        List<FeeItem> feeItems = feeItemRepository.findAllByDueDate(dueDate);
        if (feeItems.isEmpty()) {
            return 0;
        }

        Map<Long, FeeItem> itemsById = feeItems.stream()
                .collect(Collectors.toMap(FeeItem::getId, Function.identity()));
        List<FeeTarget> targets = feeTargetRepository.findAllByFeeItemIdInAndStatus(
                itemsById.keySet(), FeeTargetStatus.UNPAID);
        Map<Long, Membership> membershipsById = membershipRepository.findAllByIdIn(
                        targets.stream().map(FeeTarget::getMembershipId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Membership::getId, Function.identity()));

        List<Notification> candidates = new ArrayList<>();
        for (FeeTarget target : targets) {
            FeeItem item = itemsById.get(target.getFeeItemId());
            Membership membership = membershipsById.get(target.getMembershipId());
            if (item == null || membership == null || membership.getStatus() != MembershipStatus.ACTIVE
                    || membership.getUserId() == null) {
                continue;
            }
            String deduplicationKey = "FEE_DUE_REMINDER:%d:%s:%d".formatted(
                    item.getId(), dueDate, membership.getUserId());
            candidates.add(Notification.create(
                    item.getOrganizationId(),
                    membership.getUserId(),
                    NotificationType.FEE_DUE_REMINDER,
                    TITLE,
                    message(item, target.getAmountDue()),
                    NotificationReferenceType.FEE_ITEM,
                    item.getId(),
                    deduplicationKey
            ));
        }

        return notificationCreator.saveNew(candidates);
    }

    private String message(FeeItem item, BigDecimal amountDue) {
        return "%s 마감일은 %s이며, 납부할 금액은 %s원입니다.".formatted(
                item.getTitle(), item.getDueDate(), new DecimalFormat("#,##0.##").format(amountDue));
    }
}
