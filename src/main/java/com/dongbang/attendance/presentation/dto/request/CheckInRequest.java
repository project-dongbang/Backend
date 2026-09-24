package com.dongbang.attendance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(@NotBlank String qrToken) {}
