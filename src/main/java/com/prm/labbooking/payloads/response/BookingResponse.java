package com.prm.labbooking.payloads.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class BookingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long labId;
    private String labName;
    private String labLocation;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String title;
    private String note;
    private String status;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
