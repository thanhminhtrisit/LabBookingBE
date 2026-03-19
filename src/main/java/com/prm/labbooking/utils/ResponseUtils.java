package com.prm.labbooking.utils;

import com.prm.labbooking.payloads.response.BaseResponse;

public class ResponseUtils {

    public static BaseResponse success(Object data, String message) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("200"); r.setMessage(message); r.setData(data);
        return r;
    }

    public static BaseResponse created(Object data, String message) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("201"); r.setMessage(message); r.setData(data);
        return r;
    }

    public static BaseResponse error(String statusCode, String message) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode(statusCode); r.setMessage(message); r.setData(null);
        return r;
    }
}
