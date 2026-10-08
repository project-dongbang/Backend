package com.dongbang.organization.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganizationPaymentAccount(
        @NotBlank @Size(max = 50) String bankName,
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[0-9-]+$") String accountNumber,
