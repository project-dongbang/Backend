package com.dongbang.finance.domain.repository;

import com.dongbang.finance.domain.FeeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeeItemRepository extends JpaRepository<FeeItem, Long> {
    Page<FeeItem> findAllByOrganizationId(Long organizationId, Pageable pageable);
    Optional<FeeItem> findByIdAndOrganizationId(Long id, Long organizationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from FeeItem item where item.id = :id and item.organizationId = :organizationId")
    Optional<FeeItem> findLockedByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);
    List<FeeItem> findAllByOrganizationIdAndDueDateBetweenOrderByDueDateAscIdAsc(
            Long organizationId, LocalDate from, LocalDate to);
    List<FeeItem> findAllByDueDate(LocalDate dueDate);
}
