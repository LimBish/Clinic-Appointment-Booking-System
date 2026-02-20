package com.clinic.security;

import com.clinic.model.entity.User;
import com.clinic.model.enums.ClinicStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.UserRepository;
import com.clinic.util.ClinicContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ClinicContextFilter — per-request filter that:
 *
 *  1. Resolves the logged-in user's Clinic and stores it in ClinicContextHolder.
 *  2. Blocks requests from users whose clinic is SUSPENDED or INACTIVE
 *     (returns 403 / redirects to a "clinic suspended" error page).
 *  3. Clears ClinicContextHolder after each request for thread safety.
 *
 * Runs after Spring Security's authentication filter chain so
 * SecurityContextHolder already has the Authentication object.
 *
 * This implements ISO/IEC 27001 access control:
 * "All clinics have their independent configuration settings" (Week 5)
 * and the SUPER_ADMIN's ability to suspend a clinic immediately blocks
 * all its users system-wide.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicContextFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {

                String email = auth.getName();
                userRepository.findByEmail(email).ifPresent(user -> {

                    if (user.getRole() == Role.SUPER_ADMIN) {
                        ClinicContextHolder.setSuperAdmin();

                    } else if (user.getClinic() != null) {
                        ClinicStatus status = user.getClinic().getStatus();

                        // Block all access for SUSPENDED or INACTIVE clinics
                        if (status == ClinicStatus.SUSPENDED || status == ClinicStatus.INACTIVE) {
                            log.warn("Blocked request from user {} — clinic {} is {}",
                                    email, user.getClinic().getName(), status);
                            try {
                                // Redirect to clinic-suspended error page
                                SecurityContextHolder.clearContext();
                                response.sendRedirect("/error/clinic-suspended");
                            } catch (IOException ex) {
                                log.error("Redirect failed", ex);
                            }
                            return;
                        }

                        ClinicContextHolder.setClinic(user.getClinic());
                    }
                });
            }

            filterChain.doFilter(request, response);

        } finally {
            // ALWAYS clear — prevents tenant bleed-over in the thread pool
            ClinicContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip static resources, favicon, and all auth/error endpoints.
        // Using startsWith("/auth/") covers /auth/login, /auth/logout, /auth/register,
        // and prevents NoResourceFoundException for unknown /auth/** paths from
        // hitting this filter unnecessarily.
        String path = request.getServletPath();
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico")
                || path.startsWith("/auth/")
                || path.startsWith("/error/");
    }
}