package com.prm.labbooking.service.impl;

import com.prm.labbooking.emus.LabStatus;
import com.prm.labbooking.emus.NotificationType;
import com.prm.labbooking.entity.Booking;
import com.prm.labbooking.entity.Lab;
import com.prm.labbooking.payloads.request.CreateLabRequest;
import com.prm.labbooking.payloads.request.UpdateLabRequest;
import com.prm.labbooking.payloads.request.UpdateLabStatusRequest;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.payloads.response.BookingResponse;
import com.prm.labbooking.payloads.response.LabResponse;
import com.prm.labbooking.repository.BookingRepository;
import com.prm.labbooking.repository.LabRepository;
import com.prm.labbooking.service.LabService;
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
public class LabServiceImpl implements LabService {
    private final LabRepository labRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Override
    public BaseResponse getAllLabs(String statusStr) {
        List<Lab> labs;
        if (statusStr != null) {
            try { labs = labRepository.findByStatus(LabStatus.valueOf(statusStr.toUpperCase())); }
            catch (IllegalArgumentException e) { return ResponseUtils.error("400", "Status không hợp lệ"); }
        } else {
            labs = labRepository.findByStatus(LabStatus.ACTIVE);
        }
        return ResponseUtils.success(labs.stream().map(this::toLabResponse).toList(), "OK");
    }

    @Override
    public BaseResponse getLabById(Long id) {
        return labRepository.findById(id)
            .map(l -> ResponseUtils.success(toLabResponse(l), "OK"))
            .orElse(ResponseUtils.error("404", "Lab không tồn tại"));
    }

    @Override
    public BaseResponse getLabSchedule(Long labId, java.time.LocalDate date) {
        if (!labRepository.existsById(labId)) return ResponseUtils.error("404", "Lab không tồn tại");
        List<Booking> bookings = bookingRepository.findApprovedByLabAndDate(labId, date);
        return ResponseUtils.success(bookings.stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse createLab(CreateLabRequest request) {
        if (labRepository.findByCode(request.getCode()).isPresent())
            return ResponseUtils.error("409", "Mã lab đã tồn tại");
        Lab lab = Lab.builder().name(request.getName()).code(request.getCode())
            .location(request.getLocation()).description(request.getDescription())
            .capacity(request.getCapacity()).status(LabStatus.ACTIVE).build();
        return ResponseUtils.created(toLabResponse(labRepository.save(lab)), "Tạo lab thành công");
    }

    @Override
    public BaseResponse updateLab(Long id, UpdateLabRequest request) {
        Lab lab = labRepository.findById(id).orElse(null);
        if (lab == null) return ResponseUtils.error("404", "Lab không tồn tại");
        if (request.getName() != null) lab.setName(request.getName());
        if (request.getLocation() != null) lab.setLocation(request.getLocation());
        if (request.getDescription() != null) lab.setDescription(request.getDescription());
        if (request.getCapacity() != null) lab.setCapacity(request.getCapacity());
        return ResponseUtils.success(toLabResponse(labRepository.save(lab)), "Cập nhật thành công");
    }

    @Override
    @Transactional
    public BaseResponse updateLabStatus(Long id, UpdateLabStatusRequest request) {
        Lab lab = labRepository.findById(id).orElse(null);
        if (lab == null) return ResponseUtils.error("404", "Lab không tồn tại");
        lab.setStatus(request.getStatus());
        labRepository.save(lab);

        int affected = 0;
        if (request.getStatus() == LabStatus.MAINTENANCE || request.getStatus() == LabStatus.CLOSED) {
            List<Booking> futures = bookingRepository.findFutureActiveByLabId(id, LocalDateTime.now());
            NotificationType nType = request.getStatus() == LabStatus.MAINTENANCE
                ? NotificationType.LAB_MAINTENANCE : NotificationType.LAB_CLOSED;
            String nTitle = "Lab " + lab.getName() + " tạm ngừng hoạt động";
            String nMsg = request.getMessage() != null ? request.getMessage()
                : "Booking của bạn đã bị hủy do lab tạm ngừng.";
            for (Booking b : futures) {
                b.setStatus(com.prm.labbooking.emus.BookingStatus.CANCELLED);
                bookingRepository.save(b);
                notificationService.sendToUser(b.getUser(), b, nType, nTitle, nMsg);
                affected++;
            }
        }
        return ResponseUtils.success(Map.of("lab", toLabResponse(lab), "affectedBookings", affected),
            "Cập nhật trạng thái thành công");
    }

    private LabResponse toLabResponse(Lab l) {
        return LabResponse.builder().id(l.getId()).name(l.getName()).code(l.getCode())
            .location(l.getLocation()).description(l.getDescription()).capacity(l.getCapacity())
            .status(l.getStatus().name()).createdAt(l.getCreatedAt()).build();
    }

    private BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder().id(b.getId())
            .userId(b.getUser().getId()).userName(b.getUser().getFullName())
            .labId(b.getLab().getId()).labName(b.getLab().getName()).labLocation(b.getLab().getLocation())
            .startTime(b.getStartTime()).endTime(b.getEndTime())
            .title(b.getTitle()).note(b.getNote())
            .status(b.getStatus().name()).createdAt(b.getCreatedAt()).build();
    }
}
