package com.restauranthub.service;

import com.restauranthub.dto.*;
import com.restauranthub.entity.PasswordResetToken;
import com.restauranthub.entity.Role;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.RoleName;
import com.restauranthub.entity.enums.UserStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.exception.UnauthorizedException;
import com.restauranthub.repository.PasswordResetTokenRepository;
import com.restauranthub.repository.RoleRepository;
import com.restauranthub.repository.UserRepository;
import com.restauranthub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public User register(RegisterRequestDto request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("DUPLICATE_EMAIL", "An account with this email address already exists.");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER.name())
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_CUSTOMER.name())
                        .description("Standard Registered Customer")
                        .build()));

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered new customer ID: {} ({})", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String token = tokenProvider.generateToken(authentication);

            List<String> roles = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return AuthResponseDto.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresInMs(tokenProvider.getExpirationMs())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .roles(roles)
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for user: {}", normalizedEmail);
            throw new BadRequestException("INVALID_CREDENTIALS", "Invalid email address or password.");
        }
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }

        if (auth.getPrincipal() instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional
    public String initiatePasswordReset(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        // Security best practice: Prevent email enumeration by returning success regardless, but only log/generate if found
        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", normalizedEmail);
            return null;
        }

        User user = userOptional.get();
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("Generated password reset token for user ID: {}", user.getId());
        return token;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("INVALID_TOKEN", "Invalid or expired password reset token."));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new BadRequestException("TOKEN_EXPIRED", "The password reset token has expired or already been utilized.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Successfully updated password via reset token for user ID: {}", user.getId());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("INVALID_CURRENT_PASSWORD", "The current password provided is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User ID: {} successfully changed their password.", userId);
    }
}
