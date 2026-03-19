package com.prm.labbooking.payloads.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private UserResponse user;
}
