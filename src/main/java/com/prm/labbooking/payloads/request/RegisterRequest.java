package com.prm.labbooking.payloads.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank(message = "Full name is required") private String fullName;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") private String password;
    private String phone;
}
