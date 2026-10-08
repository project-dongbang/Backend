package com.dongbang.finance.domain.repository;

import com.dongbang.finance.domain.FeeTarget;
import com.dongbang.finance.domain.FeeTargetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeeTargetRepository extends JpaRepository<FeeTarget, Long> {
    List<FeeTarget> findAllByFeeItemId(Long feeItemId);
    List<FeeTarget> findAllByFeeItemIdAndStatus(Long feeItemId, FeeTargetStatus status);
    List<FeeTarget> findAllByFeeItemIdIn(Collection<Long> feeItemIds);
    List<FeeTarget> findAllByFeeItemIdInAndStatus(Collection<Long> feeItemIds, FeeTargetStatus status);
    Optional<FeeTarget> findByIdAndFeeItemId(Long id, Long feeItemId);
    @Query("select target.feeItemId from FeeTarget target where target.id = :id")
    Optional<Long> findFeeItemIdByTargetId(@Param("id") Long id);
    Optional<FeeTarget> findByFeeItemIdAndMembershipId(Long feeItemId, Long membershipId);
    Page<FeeTarget> findAllByMembershipId(Long membershipId, Pageable pageable);
    Page<FeeTarget> findAllByMembershipIdAndStatus(Long membershipId, FeeTargetStatus status, Pageable pageable);
    List<FeeTarget> findAllByMembershipId(Long membershipId);
    List<FeeTarget> findAllByMembershipIdAndStatus(Long membershipId, FeeTargetStatus status);
    long countByFeeItemId(Long feeItemId);
    long countByFeeItemIdAndStatus(Long feeItemId, FeeTargetStatus status);

    @Query("""
            select new com.dongbang.finance.domain.repository.DashboardFeeTotals(
                count(target), coalesce(sum(case when target.status = com.dongbang.finance.domain.FeeTargetStatus.PAID
                    then 1L else 0L end), 0L))
            from FeeTarget target, FeeItem item
            where target.feeItemId = item.id and item.organizationId = :organizationId
            """)
    DashboardFeeTotals dashboardTotals(@Param("organizationId") Long organizationId);

    @Query("""
            select count(target) from FeeTarget target, FeeItem item
            where target.feeItemId = item.id and item.organizationId = :organizationId
              and target.membershipId = :membershipId
              and target.status = com.dongbang.finance.domain.FeeTargetStatus.UNPAID
            """)
    long countDashboardUnpaid(@Param("organizationId") Long organizationId,
                              @Param("membershipId") Long membershipId);
}
