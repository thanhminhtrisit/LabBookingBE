package com.prm.labbooking.service;

import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.payloads.request.RejectBookingRequest;

public interface BookingService {
    BaseResponse createBooking(com.prm.labbooking.payloads.request.CreateBookingRequest request, Long userId);
    BaseResponse getMyBookings(Long userId);
    BaseResponse getPendingBookings();
    BaseResponse getAllBookings();
    BaseResponse approveBooking(Long bookingId, Long reviewerId);
    BaseResponse rejectBooking(Long bookingId, Long reviewerId, RejectBookingRequest request);
    BaseResponse cancelBooking(Long bookingId, Long userId);
}
