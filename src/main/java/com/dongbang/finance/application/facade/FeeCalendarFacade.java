package com.dongbang.finance.application.facade;

import com.dongbang.finance.domain.repository.FeeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeCalendarFacade {
    private final FeeItemRepository feeItems;

    public List<FeeCalendarItem> findDeadlines(Long organizationId, LocalDate from, LocalDate to) {
        return feeItems.findAllByOrganizationIdAndDueDateBetweenOrderByDueDateAscIdAsc(organizationId, from, to)
                .stream().map(item -> new FeeCalendarItem(item.getId(), item.getTitle(), item.getDueDate())).toList();
    }

    public record FeeCalendarItem(Long feeItemId, String title, LocalDate dueDate) {}
}
