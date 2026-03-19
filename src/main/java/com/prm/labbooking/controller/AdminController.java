package com.prm.labbooking.controller;

import com.prm.labbooking.emus.Role;
import com.prm.labbooking.payloads.request.UpdateRoleRequest;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.payloads.response.UserResponse;
import com.prm.labbooking.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Quản trị hệ thống")
public class AdminController {
    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<BaseResponse> getUsers() {
        List<UserResponse> data = userRepository.findAll().stream()
            .map(u -> UserResponse.builder().id(u.getId()).fullName(u.getFullName())
                .email(u.getEmail()).phone(u.getPhone()).role(u.getRole().name())
                .createdAt(u.getCreatedAt()).build()).toList();
        BaseResponse r = new BaseResponse();
        r.setStatusCode("200"); r.setMessage("OK"); r.setData(data);
        return ResponseEntity.ok(r);
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<BaseResponse> updateRole(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateRoleRequest request) {
        BaseResponse r = new BaseResponse();
        com.prm.labbooking.entity.User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            r.setStatusCode("404"); r.setMessage("User không tồn tại");
            return ResponseEntity.ok(r);
        }
        user.setRole(request.getRole());
        userRepository.save(user);
        r.setStatusCode("200"); r.setMessage("Cập nhật role thành công");
        r.setData(Map.of("userId", id, "newRole", request.getRole().name()));
        return ResponseEntity.ok(r);
    }
}
