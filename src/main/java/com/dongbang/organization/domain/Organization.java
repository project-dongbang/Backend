package com.dongbang.organization.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "organizations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 100, unique = true)
    private String slug;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "operating_semester", length = 6)
    private String operatingSemester;

    @Column(name = "default_fee_amount", precision = 12, scale = 0)
    private BigDecimal defaultFeeAmount;

    @Column(name = "fee_bank_name", length = 50)
    private String feeBankName;

    @Column(name = "fee_account_number", length = 50)
    private String feeAccountNumber;

    @Column(name = "fee_account_holder", length = 100)
    private String feeAccountHolder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    public Organization(Long id, String name, String slug, String description, String logoUrl) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.logoUrl = logoUrl;
        this.status = OrganizationStatus.ACTIVE;
    }

    public void updateInfo(String name, String description, String logoUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (logoUrl != null) {
            this.logoUrl = logoUrl;
        }
    }

    public void updateSettings(String operatingSemester, BigDecimal defaultFeeAmount,
                               String bankName, String accountNumber, String accountHolder) {
        if (operatingSemester != null) {
            this.operatingSemester = operatingSemester;
        }
        if (defaultFeeAmount != null) {
            this.defaultFeeAmount = defaultFeeAmount;
        }
        if (bankName != null) {
            this.feeBankName = bankName;
            this.feeAccountNumber = accountNumber;
            this.feeAccountHolder = accountHolder;
        }
    }

    public void delete() {
        this.status = OrganizationStatus.DELETED;
        this.deletedAt = Instant.now();
    }
}
