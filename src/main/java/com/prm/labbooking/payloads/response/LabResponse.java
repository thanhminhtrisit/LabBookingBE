package com.prm.labbooking.payloads.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class LabResponse {
    private Long id;
    private String name;
    private String code;
    private String location;
    private String description;
    private Integer capacity;
    private String status;
    private LocalDateTime createdAt;
}
