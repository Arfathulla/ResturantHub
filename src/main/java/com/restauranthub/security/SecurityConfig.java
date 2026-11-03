package com.restauranthub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restauranthub.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/h2-console/**")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            if (uri.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                ErrorResponse error = ErrorResponse.builder()
                                        .success(false)
                                        .status(HttpStatus.UNAUTHORIZED.value())
                                        .errorCode("UNAUTHORIZED")
                                        .message("Full authentication is required to access this REST endpoint.")
                                        .path(uri)
                                        .timestamp(Instant.now())
                                        .build();
                                response.getWriter().write(objectMapper.writeValueAsString(error));
                            } else {
                                response.sendRedirect("/login?expired=true");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String uri = request.getRequestURI();
                            if (uri.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                ErrorResponse error = ErrorResponse.builder()
                                        .success(false)
                                        .status(HttpStatus.FORBIDDEN.value())
                                        .errorCode("FORBIDDEN")
                                        .message("You do not possess the required privileges to access this endpoint.")
                                        .path(uri)
                                        .timestamp(Instant.now())
                                        .build();
                                response.getWriter().write(objectMapper.writeValueAsString(error));
                            } else {
                                response.sendRedirect("/error/403");
                            }
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Static Resources & Documentation
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/static/**", "/webjars/**", "/favicon.ico",
                                "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        // Public Web Pages
                        .requestMatchers(
                                "/", "/login", "/register", "/forgot-password", "/reset-password",
                                "/branches", "/branches/*", "/menu", "/menu/*", "/about", "/contact",
                                "/actuator/health", "/actuator/info"
                        ).permitAll()
                        // Public REST Endpoints
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/public/**",
                                "/api/v1/branches", "/api/v1/branches/*",
                                "/api/v1/branches/*/menu",
                                "/api/v1/menu/search",
                                "/api/v1/menu/items/*",
                                "/api/v1/webhooks/**"
                        ).permitAll()
                        // Role-based Access Control
                        .requestMatchers("/admin/**", "/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/manager/**", "/api/v1/manager/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                        .requestMatchers("/staff/**", "/api/v1/staff/**").hasAnyAuthority("ROLE_STAFF", "ROLE_MANAGER", "ROLE_ADMIN")
                        .requestMatchers("/kitchen/**", "/api/v1/kitchen/**").hasAnyAuthority("ROLE_KITCHEN", "ROLE_MANAGER", "ROLE_ADMIN")
                        // Customer / Authenticated Operations
                        .requestMatchers(
                                "/profile/**", "/reservations/**", "/cart/**", "/checkout/**",
                                "/orders/**", "/payments/**", "/api/v1/me/**", "/api/v1/reservations/**",
                                "/api/v1/cart/**", "/api/v1/orders/**", "/api/v1/payments/**",
                                "/api/v1/tracking/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()) // Allow H2 console frames if needed
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
