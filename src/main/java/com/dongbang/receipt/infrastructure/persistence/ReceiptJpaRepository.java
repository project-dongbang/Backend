package com.dongbang.receipt.infrastructure.persistence;

import com.dongbang.receipt.domain.Receipt;
import com.dongbang.receipt.domain.repository.ReceiptRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptJpaRepository extends JpaRepository<Receipt, Long>, ReceiptRepository {
}
