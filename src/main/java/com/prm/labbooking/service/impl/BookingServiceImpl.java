package com.prm.labbooking.service.impl;

import com.prm.labbooking.emus.BookingStatus;
import com.prm.labbooking.emus.NotificationType;
import com.prm.labbooking.emus.Role;
import com.prm.labbooking.entity.Booking;
import com.prm.labbooking.entity.Lab;
import com.prm.labbooking.entity.User;
import com.prm.labbooking.payloads.request.CreateBookingRequest;
import com.prm.labbooking.payloads.request.RejectBookingRequest;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.payloads.response.BookingResponse;
import com.prm.labbooking.repository.BookingRepository;
import com.prm.labbooking.repository.LabRepository;
import com.prm.labbooking.repository.UserRepository;
import com.prm.labbooking.service.BookingService;
import com.prm.labbooking.service.NotificationService;
import com.prm.labbooking.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public BaseResponse createBooking(CreateBookingRequest req, Long userId) {
        if (!req.getStartTime().isBefore(req.getEndTime()))
            return ResponseUtils.error("400", "Thời gian bắt đầu phải trước thời gian kết thúc");

        Lab lab = labRepository.findById(req.getLabId()).orElse(null);
        if (lab == null) return ResponseUtils.error("404", "Lab không tồn tại");
        if (lab.getStatus() != com.prm.labbooking.emus.LabStatus.ACTIVE)
            return ResponseUtils.error("400", "Lab hiện không hoạt động");
        if (bookingRepository.existsConflict(req.getLabId(), req.getStartTime(), req.getEndTime()))
            return ResponseUtils.error("409", "Lab đã có booking trong khung giờ này");

        User user = userRepository.findById(userId).orElseThrow();
        Booking saved = bookingRepository.save(Booking.builder()
            .user(user).lab(lab).startTime(req.getStartTime()).endTime(req.getEndTime())
            .title(req.getTitle()).note(req.getNote()).status(BookingStatus.PENDING)
            .createdAt(LocalDateTime.now()).build());

        // Notify ADMIN + STAFF
        List<User> receivers = userRepository.findByRole(Role.ADMIN);
        receivers.addAll(userRepository.findByRole(Role.STAFF));
        for (User r : receivers) {
            notificationService.sendToUser(r, saved, NotificationType.BOOKING_IN_PROGRESS,
                "Yêu cầu booking mới",
                user.getFullName() + " đã gửi yêu cầu đặt lab " + lab.getName());
        }

        // Notify the user that booking was submitted
        notificationService.sendToUser(user, saved, NotificationType.BOOKING_IN_PROGRESS,
            "Booking đã được gửi",
            "Yêu cầu đặt lab " + lab.getName() + " của bạn đã được gửi và đang chờ duyệt.");

        return ResponseUtils.created(toBookingResponse(saved), "Gửi yêu cầu booking thành công");
    }

    @Override
    public BaseResponse getMyBookings(Long userId) {
        return ResponseUtils.success(
            bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse getPendingBookings() {
        return ResponseUtils.success(
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING)
                .stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse getAllBookings() {
        return ResponseUtils.success(
            bookingRepository.findAll().stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse approveBooking(Long bookingId, Long reviewerId) {
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null) return ResponseUtils.error("404", "Booking không tồn tại");
        if (b.getStatus() != BookingStatus.PENDING)
            return ResponseUtils.error("400", "Chỉ có thể duyệt booking đang PENDING");
        if (bookingRepository.existsConflict(b.getLab().getId(), b.getStartTime(), b.getEndTime()))
            return ResponseUtils.error("409", "Khung giờ này đã bị booking khác chiếm trước");

        User reviewer = userRepository.findById(reviewerId).orElseThrow();
        b.setStatus(BookingStatus.APPROVED);
        b.setReviewedBy(reviewer);
        b.setReviewedAt(LocalDateTime.now());
        bookingRepository.save(b);

        notificationService.sendToUser(b.getUser(), b, NotificationType.BOOKING_APPROVED,
            "Booking được duyệt",
            "Booking lab " + b.getLab().getName() + " của bạn đã được duyệt.");
        return ResponseUtils.success(toBookingResponse(b), "Duyệt booking thành công");
    }

    @Override
    public BaseResponse rejectBooking(Long bookingId, Long reviewerId, RejectBookingRequest request) {
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null) return ResponseUtils.error("404", "Booking không tồn tại");
        if (b.getStatus() != BookingStatus.PENDING)
            return ResponseUtils.error("400", "Chỉ có thể từ chối booking đang PENDING");

        User reviewer = userRepository.findById(reviewerId).orElseThrow();
        b.setStatus(BookingStatus.REJECTED);
        b.setReviewedBy(reviewer);
        b.setReviewedAt(LocalDateTime.now());
        bookingRepository.save(b);

        String reason = (request != null && request.getReason() != null)
            ? request.getReason() : "Không có lý do cụ thể";
        notificationService.sendToUser(b.getUser(), b, NotificationType.BOOKING_REJECTED,
            "Booking bị từ chối",
            "Booking lab " + b.getLab().getName() + " bị từ chối. Lý do: " + reason);
        return ResponseUtils.success(toBookingResponse(b), "Từ chối booking thành công");
    }

    @Override
    public BaseResponse cancelBooking(Long bookingId, Long userId) {
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null) return ResponseUtils.error("404", "Booking không tồn tại");
        if (!b.getUser().getId().equals(userId))
            return ResponseUtils.error("403", "Bạn không có quyền hủy booking này");
        if (b.getStatus() == BookingStatus.REJECTED || b.getStatus() == BookingStatus.CANCELLED)
            return ResponseUtils.error("400", "Booking không thể hủy ở trạng thái hiện tại");

        b.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(b);

        List<User> receivers = userRepository.findByRole(Role.ADMIN);
        receivers.addAll(userRepository.findByRole(Role.STAFF));
        for (User r : receivers) {
            notificationService.sendToUser(r, b, NotificationType.BOOKING_CANCELLED,
                "Booking bị hủy",
                b.getUser().getFullName() + " đã hủy booking lab " + b.getLab().getName());
        }
        return ResponseUtils.success(toBookingResponse(b), "Hủy booking thành công");
    }

    private BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder().id(b.getId())
            .userId(b.getUser().getId()).userName(b.getUser().getFullName())
            .labId(b.getLab().getId()).labName(b.getLab().getName()).labLocation(b.getLab().getLocation())
            .startTime(b.getStartTime()).endTime(b.getEndTime())
            .title(b.getTitle()).note(b.getNote()).status(b.getStatus().name())
            .reviewedByName(b.getReviewedBy() != null ? b.getReviewedBy().getFullName() : null)
            .reviewedAt(b.getReviewedAt()).createdAt(b.getCreatedAt()).build();
    }
}
