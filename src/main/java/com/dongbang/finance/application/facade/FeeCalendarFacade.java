package com.dongbang.finance.application.facade;

import com.dongbang.finance.domain.FeeCategory;
import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.repository.FeeCategoryRepository;
import com.dongbang.finance.domain.repository.FeeItemRepository;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeCalendarFacade {
    private final FeeItemRepository feeItems;
    private final FeeCategoryRepository feeCategories;
    private final FeeTargetRepository feeTargets;

    public List<FeeCalendarItem> findDeadlines(Long organizationId, LocalDate from, LocalDate to) {
        var items = feeItems.findAllByOrganizationIdAndDueDateBetweenOrderByDueDateAscIdAsc(organizationId, from, to);
        if (items.isEmpty()) return List.of();
        var itemIds = items.stream().map(item -> item.getId()).toList();
        Map<Long, List<FeeCategory>> categoriesByItem = feeCategories.findAllByFeeItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(FeeCategory::getFeeItemId));
        var targetsByItem = feeTargets.findAllByFeeItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(target -> target.getFeeItemId()));
        return items.stream().map(item -> {
                    List<BigDecimal> amounts = categoriesByItem.getOrDefault(item.getId(), List.of()).stream()
                            .map(FeeCategory::getAmount).distinct().sorted().toList();
                    var targets = targetsByItem.getOrDefault(item.getId(), List.of());
                    long targetCount = targets.size();
                    long paidCount = targets.stream().filter(target -> target.getStatus() == FeeTargetStatus.PAID).count();
                    return new FeeCalendarItem(item.getId(), item.getTitle(), item.getDueDate(), item.getDescription(),
                            amounts.size() == 1 ? amounts.getFirst() : null, amounts,
                            targetCount, paidCount, targetCount - paidCount);
                }).toList();
    }

    public record FeeCalendarItem(Long feeItemId, String title, LocalDate dueDate, String description,
                                  BigDecimal memberAmount, List<BigDecimal> amountOptions,
                                  long targetCount, long paidCount, long unpaidCount) {}
}
