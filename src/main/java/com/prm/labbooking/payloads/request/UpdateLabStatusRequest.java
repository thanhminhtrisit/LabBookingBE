package com.prm.labbooking.payloads.request;

import com.prm.labbooking.emus.LabStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateLabStatusRequest {
    @NotNull(message = "Status is required") private LabStatus status;
    private String message;
}
