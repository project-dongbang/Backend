package com.dongbang.finance.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "fee_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeItem extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_item_id") private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(nullable = false, length = 200) private String title;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(length = 1000) private String description;
    @Column(name = "bank_name", nullable = false, length = 50) private String bankName;
    @Column(name = "bank_account_number", nullable = false, length = 255) private String encryptedAccountNumber;
    @Column(name = "account_holder", nullable = false, length = 100) private String accountHolder;
    @Column(name = "created_by_membership_id", nullable = false) private Long createdByMembershipId;

    public FeeItem(Long organizationId, String title, LocalDate dueDate, String description,
                   String bankName, String encryptedAccountNumber, String accountHolder, Long creatorId) {
        this.organizationId = organizationId; this.title = title; this.dueDate = dueDate;
        this.description = description; this.bankName = bankName;
        this.encryptedAccountNumber = encryptedAccountNumber; this.accountHolder = accountHolder;
        this.createdByMembershipId = creatorId;
    }

    public void update(String title, LocalDate dueDate, String description,
                       String bankName, String encryptedAccountNumber, String accountHolder, boolean descriptionPresent) {
        if (title != null) this.title = title;
        if (dueDate != null) this.dueDate = dueDate;
        if (descriptionPresent) this.description = description;
        if (bankName != null) {
            this.bankName = bankName;
            this.encryptedAccountNumber = encryptedAccountNumber;
            this.accountHolder = accountHolder;
        }
    }
}
