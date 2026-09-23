package com.dongbang.finance.domain.repository;

import com.dongbang.finance.domain.FeeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeeItemRepository extends JpaRepository<FeeItem, Long> {
    Page<FeeItem> findAllByOrganizationId(Long organizationId, Pageable pageable);
    Optional<FeeItem> findByIdAndOrganizationId(Long id, Long organizationId);
}
