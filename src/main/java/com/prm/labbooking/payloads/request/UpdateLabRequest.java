package com.prm.labbooking.payloads.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateLabRequest {
    private String name;
    private String location;
    private String description;
    private Integer capacity;
}
