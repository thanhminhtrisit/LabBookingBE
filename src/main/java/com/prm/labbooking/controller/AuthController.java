package com.prm.labbooking.controller;

import com.prm.labbooking.payloads.request.LoginRequest;
import com.prm.labbooking.payloads.request.RegisterRequest;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Đăng ký, đăng nhập")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<BaseResponse> me() {
        return ResponseEntity.ok(authService.getMe(getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Assuming UserPrincipal has getId()
        if (principal instanceof com.prm.labbooking.security.UserPrincipal) {
            return ((com.prm.labbooking.security.UserPrincipal) principal).getId();
        }
        throw new RuntimeException("Invalid principal");
    }
}
