package com.dongbang.finance.domain.repository;

import com.dongbang.finance.domain.FeeTarget;
import com.dongbang.finance.domain.FeeTargetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeeTargetRepository extends JpaRepository<FeeTarget, Long> {
    List<FeeTarget> findAllByFeeItemId(Long feeItemId);
    List<FeeTarget> findAllByFeeItemIdAndStatus(Long feeItemId, FeeTargetStatus status);
    List<FeeTarget> findAllByFeeItemIdIn(Collection<Long> feeItemIds);
    List<FeeTarget> findAllByFeeItemIdInAndStatus(Collection<Long> feeItemIds, FeeTargetStatus status);
    Optional<FeeTarget> findByIdAndFeeItemId(Long id, Long feeItemId);
    Page<FeeTarget> findAllByMembershipId(Long membershipId, Pageable pageable);
    Page<FeeTarget> findAllByMembershipIdAndStatus(Long membershipId, FeeTargetStatus status, Pageable pageable);
    List<FeeTarget> findAllByMembershipId(Long membershipId);
    List<FeeTarget> findAllByMembershipIdAndStatus(Long membershipId, FeeTargetStatus status);
    long countByFeeItemId(Long feeItemId);
    long countByFeeItemIdAndStatus(Long feeItemId, FeeTargetStatus status);
}
