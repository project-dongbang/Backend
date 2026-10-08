package com.dongbang.organization.presentation.dto.request;

import com.dongbang.organization.presentation.dto.OrganizationPaymentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateOrganizationSettingsRequest(
        @Pattern(regexp = "^[0-9]{4}-[12]$", message = "운영 학기는 YYYY-1 또는 YYYY-2 형식이어야 합니다.")
        String operatingSemester,
        @DecimalMin(value = "0", message = "기본 회비는 0원 이상이어야 합니다.")
        @Digits(integer = 12, fraction = 0, message = "기본 회비는 원 단위의 정수여야 합니다.")
        BigDecimal defaultFeeAmount,
        @Valid OrganizationPaymentAccount paymentAccount
) {
}
