package com.prm.labbooking.service;

import com.prm.labbooking.payloads.response.BaseResponse;

public interface LabService {
    BaseResponse getAllLabs(String status);
    BaseResponse getLabById(Long id);
    BaseResponse getLabSchedule(Long labId, java.time.LocalDate date);
    BaseResponse createLab(com.prm.labbooking.payloads.request.CreateLabRequest request);
    BaseResponse updateLab(Long id, com.prm.labbooking.payloads.request.UpdateLabRequest request);
    BaseResponse updateLabStatus(Long id, com.prm.labbooking.payloads.request.UpdateLabStatusRequest request);
}
