package com.restauranthub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restauranthub.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;

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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/**", "/h2-console/**",
                                "/sql-queries/**", "/admin/sql-console/**", "/sql/**",
                                "/orders/*/advance", "/orders/*/cancel"
                        )
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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public Static Resources & Documentation
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/static/**", "/webjars/**", "/favicon.ico",
                                "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        // Public Web Pages
                        .requestMatchers(
                                "/", "/login", "/register", "/forgot-password", "/reset-password",
                                "/branches", "/branches/*", "/menu", "/menu/*", "/about", "/contact",
                                "/orders/tracking", "/tracking", "/delivery-tracker",
                                "/orders/*/advance", "/orders/*/cancel",
                                "/sql-queries", "/sql-queries/**", "/sql", "/sql/**", "/admin/sql-console", "/admin/sql-console/**",
                                "/api/v1/admin/sql/**", "/api/admin/sql/**",
                                "/api/v1/orders/*/advance", "/api/v1/orders/*/status/advance",
                                "/api/orders/*/advance", "/api/orders/*/status/advance",
                                "/actuator/health", "/actuator/info"
                        ).permitAll()
                        // Public REST Endpoints
                        .requestMatchers(
                                "/api/v1/auth/**", "/api/auth/**",
                                "/api/v1/public/**", "/api/public/**",
                                "/api/v1/branches", "/api/v1/branches/**",
                                "/api/branches", "/api/branches/**",
                                "/api/v1/menu/**", "/api/menu/**",
                                "/api/v1/tracking/**", "/api/tracking/**",
                                "/api/v1/webhooks/**", "/api/webhooks/**"
                        ).permitAll()
                        // Role-based Access Control
                        .requestMatchers("/admin/**", "/sql-console/**", "/api/v1/admin/**", "/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/manager/**", "/api/v1/manager/**", "/api/manager/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                        .requestMatchers("/staff/**", "/api/v1/staff/**", "/api/staff/**").hasAnyAuthority("ROLE_STAFF", "ROLE_MANAGER", "ROLE_ADMIN")
                        .requestMatchers("/kitchen/**", "/api/v1/kitchen/**", "/api/kitchen/**").hasAnyAuthority("ROLE_KITCHEN", "ROLE_MANAGER", "ROLE_ADMIN")
                        // Customer / Authenticated Operations
                        .requestMatchers(
                                "/profile/**", "/reservations/**", "/cart/**", "/checkout/**",
                                "/orders/**", "/payments/**", "/api/v1/me/**", "/api/me/**",
                                "/api/v1/reservations/**", "/api/reservations/**",
                                "/api/v1/cart/**", "/api/cart/**",
                                "/api/v1/orders/**", "/api/orders/**",
                                "/api/v1/payments/**", "/api/payments/**",
                                "/api/v1/tracking/**", "/api/tracking/**"
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
