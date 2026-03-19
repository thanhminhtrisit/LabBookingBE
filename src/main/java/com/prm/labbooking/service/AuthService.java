package com.prm.labbooking.service;

import com.prm.labbooking.payloads.response.BaseResponse;

public interface AuthService {
    BaseResponse register(com.prm.labbooking.payloads.request.RegisterRequest request);
    BaseResponse login(com.prm.labbooking.payloads.request.LoginRequest request);
    BaseResponse getMe(Long userId);
}
