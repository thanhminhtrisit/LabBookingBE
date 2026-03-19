package com.prm.labbooking.payloads.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class CreateBookingRequest {
    @NotNull private Long labId;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    @NotBlank private String title;
    private String note;
}
