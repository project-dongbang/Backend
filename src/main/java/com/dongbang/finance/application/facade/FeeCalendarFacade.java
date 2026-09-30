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
        // 캘린더 목록용 회비 식별 정보
        var items = feeItems.findAllByOrganizationIdAndDueDateBetweenOrderByDueDateAscIdAsc(organizationId, from, to);
        return items.stream()
                .map(item -> new FeeCalendarItem(item.getId(), item.getTitle(), item.getDueDate()))
                .toList();
    }

    public record FeeCalendarItem(Long feeItemId, String title, LocalDate dueDate) {}
}
