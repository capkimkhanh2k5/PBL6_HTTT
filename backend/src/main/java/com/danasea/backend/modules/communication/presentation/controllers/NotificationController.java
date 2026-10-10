package com.danasea.backend.modules.communication.presentation.controllers;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.danasea.backend.modules.communication.application.usecases.GetNotificationsUseCase;
import com.danasea.backend.modules.communication.application.usecases.GetUnreadNotificationCountUseCase;
import com.danasea.backend.modules.communication.application.usecases.MarkAllNotificationsReadUseCase;
import com.danasea.backend.modules.communication.application.usecases.MarkNotificationReadUseCase;
import com.danasea.backend.modules.communication.presentation.dtos.MarkAllNotificationsReadResponse;
import com.danasea.backend.modules.communication.presentation.dtos.NotificationResponse;
import com.danasea.backend.modules.communication.presentation.dtos.UnreadCountResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        return ResponseEntity.ok(getNotificationsUseCase.execute(userId, page, size));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        return ResponseEntity.ok(markNotificationReadUseCase.execute(id, userId));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarkAllNotificationsReadResponse> markAllAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        int updated = markAllNotificationsReadUseCase.execute(userId);
        return ResponseEntity.ok(new MarkAllNotificationsReadResponse(updated));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        long count = getUnreadNotificationCountUseCase.execute(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }
}
