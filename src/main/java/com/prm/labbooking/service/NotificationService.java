package com.prm.labbooking.service;

import com.prm.labbooking.payloads.response.BaseResponse;

public interface NotificationService {
    void sendToUser(com.prm.labbooking.entity.User user, com.prm.labbooking.entity.Booking booking, com.prm.labbooking.emus.NotificationType type, String title, String message);
    BaseResponse getMyNotifications(Long userId);
    BaseResponse markAsRead(Long notificationId, Long userId);
    BaseResponse markAllAsRead(Long userId);
}
