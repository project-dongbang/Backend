package com.dongbang.finance.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeCategory extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_category_id") private Long id;
    @Column(name = "fee_item_id", nullable = false) private Long feeItemId;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;

    public FeeCategory(Long feeItemId, String name, BigDecimal amount, int displayOrder) {
        this.feeItemId = feeItemId; this.name = name; this.amount = amount; this.displayOrder = displayOrder;
    }
}
