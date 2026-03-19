package com.prm.labbooking.controller;

import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thông báo")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<BaseResponse> getAll() {
        return ResponseEntity.ok(notificationService.getMyNotifications(getCurrentUserId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<BaseResponse> read(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id, getCurrentUserId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<BaseResponse> readAll() {
        return ResponseEntity.ok(notificationService.markAllAsRead(getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.prm.labbooking.security.UserPrincipal) {
            return ((com.prm.labbooking.security.UserPrincipal) principal).getId();
        }
        throw new RuntimeException("Invalid principal");
    }
}
