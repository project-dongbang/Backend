package com.dongbang.mypage.presentation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(min = 1, max = 100) String name,
        @Size(min = 1, max = 20) String studentNumber,
        @Size(min = 1, max = 100) String department,
        @Email @Size(max = 255) String email
) {
    @AssertTrue(message = "수정할 필드를 한 개 이상 입력해야 합니다.")
    public boolean isAnyFieldProvided() {
        return name != null || studentNumber != null || department != null || email != null;
    }
}
