package com.restauranthub.controller;

import com.restauranthub.dto.*;
import com.restauranthub.entity.User;
import com.restauranthub.service.AuthService;
import com.restauranthub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity", description = "Endpoints for user registration, token-based login, password recovery, and profile inspection")
public class AuthRestController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<UserProfileDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        User user = authService.register(request);
        UserProfileDto profile = userService.getUserProfile(user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account registered successfully. You may now log in.", profile));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT Bearer token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        UserProfileDto profile = userService.getUserProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset workflow")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        String token = authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If the email exists, a password reset link has been dispatched.",
                token != null ? "Token created: " + token : "Handled"
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete password reset using token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You may now log in.", null));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password for currently authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password successfully changed.", null));
    }
}
