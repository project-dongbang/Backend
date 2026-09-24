package com.dongbang.notification.presentation;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import com.dongbang.notification.application.NotificationService;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationListResult;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationReadAllResult;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationReadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "알림", description = "내 알림 조회 및 읽음 처리 API")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "내 알림 목록과 미읽음 개수 조회")
    public ApiResponse<NotificationListResult> getNotifications(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @RequestParam @Positive Long organizationId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                notificationService.getNotifications(organizationId, userId, unreadOnly, cursor, size));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "내 알림 읽음 처리")
    public ApiResponse<NotificationReadResult> markRead(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long notificationId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                notificationService.markRead(notificationId, userId));
    }

    @PostMapping("/read-all")
    @Operation(summary = "현재 동아리 알림 모두 읽음 처리")
    public ApiResponse<NotificationReadAllResult> markAllRead(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @RequestParam @Positive Long organizationId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                notificationService.markAllRead(organizationId, userId));
    }
}
