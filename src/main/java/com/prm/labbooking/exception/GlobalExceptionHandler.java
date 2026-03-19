package com.prm.labbooking.exception;

import com.prm.labbooking.payloads.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        BaseResponse r = new BaseResponse();
        r.setStatusCode("400"); r.setMessage(message);
        return ResponseEntity.badRequest().body(r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse> handleAccessDenied(AccessDeniedException ex) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("403"); r.setMessage("Bạn không có quyền thực hiện hành động này");
        return ResponseEntity.status(403).body(r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGeneral(Exception ex) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("500"); r.setMessage("Lỗi server: " + ex.getMessage());
        return ResponseEntity.status(500).body(r);
    }
}
