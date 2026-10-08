package com.dongbang.organization.presentation.dto.response;

import com.dongbang.organization.presentation.dto.OrganizationPaymentAccount;

import java.math.BigDecimal;

public record OrganizationSettingsResponse(
        String operatingSemester,
        BigDecimal defaultFeeAmount,
        OrganizationPaymentAccount paymentAccount
) {
}
