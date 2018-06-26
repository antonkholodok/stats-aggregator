package com.akholodok.stats.aggregator.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class RestErrorHandler extends ResponseEntityExceptionHandler {

    private class ErrorResponse {

        private final long timestamp;
        private final String message;
        private final int status;
        private final Map<String, List<String>> errors;

        public ErrorResponse(long timestamp,
                             String message,
                             int status,
                             Map<String, List<String>> errors) {
            this.timestamp = timestamp;
            this.message = message;
            this.status = status;
            this.errors = errors;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }

        public Map<String, List<String>> getErrors() {
            return errors;
        }
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        Map<String, List<String>> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors
                .computeIfAbsent(error.getField(), s -> new LinkedList<>())
                .add(error.getDefaultMessage());
        }
        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now().toEpochMilli(),
            status.getReasonPhrase(),
            status.value(),
            errors);
        return new ResponseEntity<>(errorResponse, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatus status,
                                                             WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now().toEpochMilli(),
            status.getReasonPhrase(),
            status.value(),
            Collections.emptyMap());
        return new ResponseEntity<>(errorResponse, headers, status);
    }
}
