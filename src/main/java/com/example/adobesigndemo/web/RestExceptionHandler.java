package com.example.adobesigndemo.web;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.adobesigndemo.service.AdobeSignClientException;
import com.example.adobesigndemo.web.dto.ApiError;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(AdobeSignClientException.class)
    public ResponseEntity<ApiError> handleAdobeSignClientException(AdobeSignClientException ex) {
        final ApiError error = new ApiError(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        final String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }
}
