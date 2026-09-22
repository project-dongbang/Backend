package com.dongbang.receipt.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id") private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "uploaded_by_membership_id", nullable = false) private Long uploadedByMembershipId;
    @Column(name = "storage_key", nullable = false, length = 500) private String storageKey;
    @Column(name = "original_name", nullable = false) private String originalName;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "size_bytes", nullable = false) private Long sizeBytes;
    @Column(name = "checksum", length = 128) private String checksum;
    @Column(name = "ocr_text", nullable = false, columnDefinition = "TEXT") private String ocrText;
    @Column(name = "store_name") private String storeName;
    @Column(name = "purchased_at") private LocalDate purchasedAt;
    @Column(name = "total_amount", precision = 15, scale = 2) private BigDecimal totalAmount;
    @Column(name = "item_title", length = 100) private String itemTitle;
    @Column(name = "category", length = 50) private String category;
    @Column(name = "payment_method", length = 50) private String paymentMethod;
    @Column(name = "memo", columnDefinition = "TEXT") private String memo;
    @Column(name = "items_json", nullable = false, columnDefinition = "TEXT") private String itemsJson;

    @Builder
    public Receipt(Long organizationId, Long uploadedByMembershipId, String storageKey, String originalName,
                   String contentType, Long sizeBytes, String checksum, String ocrText,
                   String storeName, LocalDate purchasedAt, BigDecimal totalAmount, String itemTitle,
                   String category, String paymentMethod, String memo, String itemsJson) {
        this.organizationId = organizationId; this.uploadedByMembershipId = uploadedByMembershipId;
        this.storageKey = storageKey; this.originalName = originalName; this.contentType = contentType;
        this.sizeBytes = sizeBytes; this.checksum = checksum; this.ocrText = ocrText;
        this.storeName = storeName; this.purchasedAt = purchasedAt; this.totalAmount = totalAmount;
        this.itemTitle = itemTitle; this.category = category; this.paymentMethod = paymentMethod;
        this.memo = memo; this.itemsJson = itemsJson;
    }
}
