package com.dongbang.receipt.domain.repository;

import com.dongbang.receipt.domain.Receipt;

public interface ReceiptRepository {
    Receipt save(Receipt receipt);
}
