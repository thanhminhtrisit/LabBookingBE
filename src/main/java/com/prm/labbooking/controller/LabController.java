package com.prm.labbooking.controller;

import com.prm.labbooking.payloads.request.CreateLabRequest;
import com.prm.labbooking.payloads.request.UpdateLabRequest;
import com.prm.labbooking.payloads.request.UpdateLabStatusRequest;
import com.prm.labbooking.payloads.response.BaseResponse;
import com.prm.labbooking.service.LabService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/labs")
@RequiredArgsConstructor
@Tag(name = "Labs", description = "Quản lý lab")
public class LabController {
    private final LabService labService;

    @GetMapping
    public ResponseEntity<BaseResponse> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(labService.getAllLabs(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(labService.getLabById(id));
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<BaseResponse> schedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(labService.getLabSchedule(id, date));
    }

    @PostMapping
    public ResponseEntity<BaseResponse> create(@Valid @RequestBody CreateLabRequest req) {
        return ResponseEntity.ok(labService.createLab(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse> update(@PathVariable Long id,
                                                @RequestBody UpdateLabRequest req) {
        return ResponseEntity.ok(labService.updateLab(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BaseResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateLabStatusRequest req) {
        return ResponseEntity.ok(labService.updateLabStatus(id, req));
    }
}
