package com.dongbang.finance.domain.repository;

import com.dongbang.finance.domain.FinancialTransaction;
import com.dongbang.finance.domain.TransactionStatus;
import com.dongbang.finance.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    List<FinancialTransaction> findAllByOrganizationIdAndStatusOrderByOccurredOnDescIdDesc(Long organizationId, TransactionStatus status);
    Optional<FinancialTransaction> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<FinancialTransaction> findFirstByFeeTargetIdAndTransactionTypeAndStatus(Long feeTargetId, TransactionType type, TransactionStatus status);
    List<FinancialTransaction> findAllByFeeItemId(Long feeItemId);
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM FinancialTransaction t, UploadedFile f " +
            "WHERE t.organizationId = :organizationId AND t.evidenceFileId = f.id AND f.checksum = :checksum " +
            "AND f.deletedAt IS NULL AND t.status = :status")
    boolean existsEvidenceChecksum(@Param("organizationId") Long organizationId,
                                   @Param("checksum") String checksum,
                                   @Param("status") TransactionStatus status);
}
