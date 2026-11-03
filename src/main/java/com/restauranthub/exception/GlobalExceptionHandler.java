package com.restauranthub.exception;

import com.restauranthub.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        return uri.startsWith("/api/") || (acceptHeader != null && acceptHeader.contains("application/json"));
    }

    @ExceptionHandler(ApiException.class)
    public Object handleApiException(ApiException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] API Exception [{}]: {}", correlationId, ex.getErrorCode(), ex.getMessage());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .success(false)
                    .status(ex.getStatus().value())
                    .errorCode(ex.getErrorCode())
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .correlationId(correlationId)
                    .timestamp(Instant.now())
                    .build();
            return new ResponseEntity<>(error, ex.getStatus());
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", ex.getStatus().value());
        mav.addObject("error", ex.getErrorCode());
        mav.addObject("message", ex.getMessage());
        mav.addObject("correlationId", correlationId);
        return mav;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Validation error: {}", correlationId, ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
                    Object rejectedVal = (error instanceof FieldError fe) ? fe.getRejectedValue() : null;
                    return new ErrorResponse.ValidationError(
                            fieldName,
                            rejectedVal != null ? rejectedVal.toString() : null,
                            error.getDefaultMessage()
                    );
                })
                .collect(Collectors.toList());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .success(false)
                    .status(HttpStatus.BAD_REQUEST.value())
                    .errorCode("VALIDATION_ERROR")
                    .message("Input validation failed for one or more fields")
                    .path(request.getRequestURI())
                    .correlationId(correlationId)
                    .timestamp(Instant.now())
                    .validationErrors(validationErrors)
                    .build();
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", HttpStatus.BAD_REQUEST.value());
        mav.addObject("error", "VALIDATION_ERROR");
        mav.addObject("message", "Input validation failed. Please review your entries.");
        mav.addObject("correlationId", correlationId);
        return mav;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Constraint violation: {}", correlationId, ex.getMessage());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .success(false)
                    .status(HttpStatus.BAD_REQUEST.value())
                    .errorCode("CONSTRAINT_VIOLATION")
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .correlationId(correlationId)
                    .timestamp(Instant.now())
                    .build();
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", HttpStatus.BAD_REQUEST.value());
        mav.addObject("error", "CONSTRAINT_VIOLATION");
        mav.addObject("message", ex.getMessage());
        mav.addObject("correlationId", correlationId);
        return mav;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Access denied on {}: {}", correlationId, request.getRequestURI(), ex.getMessage());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .success(false)
                    .status(HttpStatus.FORBIDDEN.value())
                    .errorCode("ACCESS_DENIED")
                    .message("You do not have the required permissions to access this resource.")
                    .path(request.getRequestURI())
                    .correlationId(correlationId)
                    .timestamp(Instant.now())
                    .build();
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }

        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("status", 403);
        mav.addObject("error", "Forbidden");
        mav.addObject("message", "You do not have permission to access this resource.");
        mav.addObject("correlationId", correlationId);
        return mav;
    }

    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Authentication failure: {}", correlationId, ex.getMessage());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .success(false)
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .errorCode("AUTHENTICATION_FAILED")
                    .message("Invalid credentials or authentication token.")
                    .path(request.getRequestURI())
                    .correlationId(correlationId)
                    .timestamp(Instant.now())
                    .build();
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        ModelAndView mav = new ModelAndView("login");
        mav.addObject("error", "Invalid email or password");
        return mav;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] Unhandled system error on {}: {}", correlationId, request.getRequestURI(), ex.getMessage(), ex);

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .errorCode("INTERNAL_SERVER_ERROR")
                    .message("An unexpected error occurred. Please quote correlation ID: " + correlationId)
                    .path(request.getRequestURI())
                    .correlationId(correlationId)
                    .timestamp(Instant.now())
                    .build();
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject("error", "INTERNAL_SERVER_ERROR");
        mav.addObject("message", "An unexpected error occurred. Please contact support with ID: " + correlationId);
        mav.addObject("correlationId", correlationId);
        return mav;
    }
}
