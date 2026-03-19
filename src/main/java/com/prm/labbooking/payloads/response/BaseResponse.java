package com.prm.labbooking.payloads.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseResponse {
    private String message;
    private String statusCode;
    private Object data;
}
