package com.prm.labbooking.payloads.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateLabRequest {
    @NotBlank
    private String name;
    @NotBlank private String code;
    private String location;
    private String description;
    private Integer capacity;
}
