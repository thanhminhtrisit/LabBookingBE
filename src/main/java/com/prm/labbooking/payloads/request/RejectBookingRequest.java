 package com.prm.labbooking.payloads.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RejectBookingRequest {
    private String reason;
}
