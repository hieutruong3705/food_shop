package com.example.shop.api.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class, org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<Map<String,Object>> missingRoute(Exception exception) {
        return error(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên", Map.of());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String,Object>> unsupportedMedia(Exception exception) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Kiểu nội dung không được hỗ trợ", Map.of());
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        NumberFormatException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<Map<String,Object>> invalidParameter(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Tham số yêu cầu không hợp lệ hoặc còn thiếu", Map.of());
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String,Object>> unsupportedMethod(Exception exception) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức yêu cầu không được hỗ trợ", Map.of());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,Object>> uploadSize(Exception exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh vượt quá giới hạn 5 MB", Map.of());
    }

    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<Map<String,Object>> concurrentUpdate(Exception exception) {
        return error(HttpStatus.CONFLICT, "Dữ liệu đang được cập nhật. Vui lòng thử lại.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> unexpected(Exception exception) {
        log.error("API request failed", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Máy chủ gặp lỗi. Vui lòng thử lại sau.", Map.of());
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> serviceStatus(org.springframework.web.server.ResponseStatusException exception) {
        return error(HttpStatus.valueOf(exception.getStatusCode().value()),exception.getReason(),Map.of());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> authentication(Exception exception) {
        return error(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không hợp lệ hoặc tài khoản chưa được kích hoạt", Map.of());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(Exception exception) {
        return error(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", Map.of());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(Exception exception) {
        return error(HttpStatus.CONFLICT, "Dữ liệu trùng hoặc đang được sử dụng", Map.of());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> malformed(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Định dạng dữ liệu không hợp lệ", Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", fields);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, Map<?, ?> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
