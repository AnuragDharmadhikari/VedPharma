package org.ved.crm.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.ved.crm.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFound(
            ResourceNotFoundException ex) {
        return ApiResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField()
                        + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ApiResponse.failure(message);
    }

    // IllegalArgumentException — bad input, wrong field value, XOR violations
    // HTTP 400 BAD REQUEST — client sent invalid data
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ApiResponse.failure(ex.getMessage());
    }

    // IllegalStateException — business rule conflict, duplicate active scheme,
    // invoice already exists for order, etc.
    // HTTP 409 CONFLICT — valid data but conflicts with current state
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleIllegalState(
            IllegalStateException ex) {
        return ApiResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleBadCredentials(
            BadCredentialsException ex) {
        return ApiResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        return ApiResponse.failure(
                "Operation violates a data constraint. " +
                        "Possible duplicate entry or invalid reference.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {
        return ApiResponse.failure(
                "Invalid request format. Check enum values and data types.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGenericException(Exception ex) {
        return ApiResponse.failure("An unexpected error occurred");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.failure(
                "Access denied. You do not have permission to perform this action.");
    }

    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleTooManyRequests(TooManyRequestsException ex) {
        return ApiResponse.failure(ex.getMessage());
    }
}