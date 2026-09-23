package com.dongbang.finance.domain.repository;

import com.dongbang.finance.domain.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {
    List<FeeCategory> findAllByFeeItemIdOrderByDisplayOrderAsc(Long feeItemId);
    List<FeeCategory> findAllByFeeItemIdIn(Collection<Long> feeItemIds);
    long countByFeeItemId(Long feeItemId);
}
