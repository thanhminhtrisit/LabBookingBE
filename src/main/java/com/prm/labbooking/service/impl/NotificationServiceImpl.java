package com.prm.labbooking.service.impl;

import com.prm.labbooking.entity.Notification;
import com.prm.labbooking.entity.User;
import com.prm.labbooking.entity.Booking;
import com.prm.labbooking.emus.NotificationType;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.payloads.response.NotificationResponse;
import com.prm.labbooking.repository.NotificationRepository;
import com.prm.labbooking.service.NotificationService;
import com.prm.labbooking.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    public void sendToUser(User user, Booking booking, NotificationType type, String title, String message) {
        notificationRepository.save(Notification.builder()
            .user(user).booking(booking).type(type)
            .title(title).message(message).isRead(false)
            .createdAt(LocalDateTime.now()).build());
    }

    @Override
    public BaseResponse getMyNotifications(Long userId) {
        List<NotificationResponse> data = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).toList();
        return ResponseUtils.success(data, "OK");
    }

    @Override
    public BaseResponse markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId).orElse(null);
        if (n == null) return ResponseUtils.error("404", "Notification không tồn tại");
        if (!n.getUser().getId().equals(userId)) return ResponseUtils.error("403", "Không có quyền");
        n.setIsRead(true);
        return ResponseUtils.success(toResponse(notificationRepository.save(n)), "OK");
    }

    @Override
    public BaseResponse markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .stream().filter(n -> !n.getIsRead()).toList();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        return ResponseUtils.success(Map.of("updatedCount", unread.size()), "Đã đánh dấu tất cả đã đọc");
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .bookingId(n.getBooking() != null ? n.getBooking().getId() : null)
            .type(n.getType().name()).title(n.getTitle()).message(n.getMessage())
            .isRead(n.getIsRead()).createdAt(n.getCreatedAt()).build();
    }
}
