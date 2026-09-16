package com.bangla.karneval.controller;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = MembershipController.class)
public class MembershipExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> expected(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "Request could not be completed" : e.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).findFirst().orElse("Please check the form");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> unreadable() { return ResponseEntity.badRequest().body(Map.of("message", "Please check dates, membership type and payment method")); }
}
