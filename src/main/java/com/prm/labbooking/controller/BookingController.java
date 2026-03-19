package com.prm.labbooking.controller;

import com.prm.labbooking.payloads.request.CreateBookingRequest;
import com.prm.labbooking.payloads.request.RejectBookingRequest;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Đặt phòng lab")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BaseResponse> create(@Valid @RequestBody CreateBookingRequest req) {
        return ResponseEntity.ok(bookingService.createBooking(req, getCurrentUserId()));
    }

    @GetMapping("/my")
    public ResponseEntity<BaseResponse> myBookings() {
        return ResponseEntity.ok(bookingService.getMyBookings(getCurrentUserId()));
    }

    @GetMapping("/pending")
    public ResponseEntity<BaseResponse> pending() {
        return ResponseEntity.ok(bookingService.getPendingBookings());
    }

    @GetMapping
    public ResponseEntity<BaseResponse> all() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<BaseResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.approveBooking(id, getCurrentUserId()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BaseResponse> reject(@PathVariable Long id,
                                                @RequestBody(required = false) RejectBookingRequest req) {
        return ResponseEntity.ok(bookingService.rejectBooking(id, getCurrentUserId(), req));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BaseResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.prm.labbooking.security.UserPrincipal) {
            return ((com.prm.labbooking.security.UserPrincipal) principal).getId();
        }
        throw new RuntimeException("Invalid principal");
    }
}
