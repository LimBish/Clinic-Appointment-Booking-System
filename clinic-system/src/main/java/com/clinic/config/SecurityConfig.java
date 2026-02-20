package com.clinic.config;

import com.clinic.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * SecurityConfig — configures Spring Security for RBAC, login, and session
 * management.
 *
 * Role-Based Access Control (RBAC) mapping:
 *
 * /auth/** — public (login, register)
 * /patient/** — PATIENT only
 * /doctor/** — DOCTOR only
 * /admin/** — ADMIN only
 * /queue/status — PATIENT (view own position)
 * /actuator/** — ADMIN only (monitoring endpoints)
 *
 * ISO/IEC 27001:
 * - BCrypt with strength 12 (industry standard cost factor)
 * - Session fixation protection enabled (default)
 * - CSRF enabled (Thymeleaf inserts tokens automatically)
 * - @EnableMethodSecurity allows @PreAuthorize on service methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ── URL Authorization Rules ────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Public pages (landing and auth)
                        .requestMatchers("/", "/auth/**", "/error/**").permitAll()
                        // Role-restricted areas
                        .requestMatchers("/superadmin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/patient/**").hasRole("PATIENT")
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated())

                // ── Form Login ─────────────────────────────────────────────────────
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        // Role-based redirect after successful login
                        .successHandler(new RoleBasedLoginSuccessHandler())
                        .failureUrl("/auth/login?error=true")
                        .permitAll())

                // ── Logout ─────────────────────────────────────────────────────────
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())

                // ── Session Management ─────────────────────────────────────────────
                .sessionManagement(session -> session
                        .maximumSessions(1) // one session per user
                        .expiredUrl("/auth/login?expired=true"))

                // ── Exception Handling ─────────────────────────────────────────────
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403"));

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength 12 — ~300ms hash time, strong against brute force
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}