package com.prm.labbooking.service.impl;

import com.prm.labbooking.emus.Role;
import com.prm.labbooking.entity.User;
import com.prm.labbooking.payloads.request.LoginRequest;
import com.prm.labbooking.payloads.request.RegisterRequest;
import com.prm.labbooking.payloads.response.AuthResponse;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.payloads.response.UserResponse;
import com.prm.labbooking.repository.UserRepository;
import com.prm.labbooking.service.AuthService;
import com.prm.labbooking.utils.JwtUtils;
import com.prm.labbooking.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public BaseResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            return ResponseUtils.error("409", "Email đã được sử dụng");

        User user = User.builder()
            .fullName(request.getFullName()).email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .studentCode(request.getStudentCode())   // NEW
            .staffCode(request.getStaffCode())       // NEW
            .department(request.getDepartment())     // NEW
            .faculty(request.getFaculty())           // NEW
            .role(Role.MEMBER)
            .createdAt(LocalDateTime.now()).build();
        User saved = userRepository.save(user);
        String token = jwtUtils.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());

        return ResponseUtils.created(
            AuthResponse.builder().token(token).tokenType("Bearer").user(toUserResponse(saved)).build(),
            "Đăng ký thành công");
    }

    @Override
    public BaseResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword()))
            return ResponseUtils.error("401", "Email hoặc mật khẩu không đúng");

        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return ResponseUtils.success(
            AuthResponse.builder().token(token).tokenType("Bearer").user(toUserResponse(user)).build(),
            "Đăng nhập thành công");
    }

    @Override
    public BaseResponse getMe(Long userId) {
        return userRepository.findById(userId)
            .map(u -> ResponseUtils.success(toUserResponse(u), "OK"))
            .orElse(ResponseUtils.error("404", "User không tồn tại"));
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
            .id(u.getId()).fullName(u.getFullName()).email(u.getEmail())
            .phone(u.getPhone()).role(u.getRole().name())
            .studentCode(u.getStudentCode())   // NEW
            .staffCode(u.getStaffCode())       // NEW
            .department(u.getDepartment())     // NEW
            .faculty(u.getFaculty())           // NEW
            .createdAt(u.getCreatedAt()).build();
    }
}
