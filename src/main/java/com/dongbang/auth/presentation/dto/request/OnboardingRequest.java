package com.dongbang.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자 이하이어야 합니다.")
        String name,

        @NotBlank(message = "학번은 필수입니다.")
        @Size(max = 20, message = "학번은 20자 이하이어야 합니다.")
        String studentNumber,

        @NotBlank(message = "학과는 필수입니다.")
        @Size(max = 100, message = "학과는 100자 이하이어야 합니다.")
        String department,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하이어야 합니다.")
        String email
) {
}
