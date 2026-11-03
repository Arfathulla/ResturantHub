package com.restauranthub;

import com.restauranthub.dto.AuthResponseDto;
import com.restauranthub.dto.LoginRequestDto;
import com.restauranthub.dto.RegisterRequestDto;
import com.restauranthub.entity.Role;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.RoleName;
import com.restauranthub.entity.enums.UserStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.repository.PasswordResetTokenRepository;
import com.restauranthub.repository.RoleRepository;
import com.restauranthub.repository.UserRepository;
import com.restauranthub.security.JwtTokenProvider;
import com.restauranthub.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder()
                .id(1L)
                .name(RoleName.ROLE_CUSTOMER.name())
                .description("Customer")
                .build();
    }

    @Test
    @DisplayName("Registration: Creates active customer user with BCrypt hashed password")
    void testRegisterSuccess() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("Alice");
        request.setLastName("Smith");
        request.setEmail("alice.smith@example.com");
        request.setPassword("SecureSecret123!");
        request.setPhoneNumber("+12025550199");

        when(userRepository.existsByEmail("alice.smith@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER.name())).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("SecureSecret123!")).thenReturn("$2a$10$hashedPasswordSample");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        User created = authService.register(request);

        assertNotNull(created);
        assertEquals("alice.smith@example.com", created.getEmail());
        assertEquals("$2a$10$hashedPasswordSample", created.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, created.getStatus());
        assertTrue(created.getRoles().contains(customerRole));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Registration: Rejects duplicate email address")
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("already.exists@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Bob");
        request.setLastName("Jones");

        when(userRepository.existsByEmail("already.exists@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                authService.register(request)
        );

        assertEquals("DUPLICATE_EMAIL", ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login: Authenticates valid credentials and generates JWT token")
    void testLoginSuccess() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("customer@restauranthub.com");
        request.setPassword("password");

        User user = User.builder()
                .id(5L)
                .email("customer@restauranthub.com")
                .firstName("Alice")
                .lastName("Walker")
                .roles(Set.of(customerRole))
                .status(UserStatus.ACTIVE)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("sample.jwt.token");

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.getToken());
        assertEquals("customer@restauranthub.com", response.getEmail());
        assertTrue(response.getRoles().contains(RoleName.ROLE_CUSTOMER.name()));
    }
}
