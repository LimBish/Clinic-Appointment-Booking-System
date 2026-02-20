package com.clinic.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * RoleBasedLoginSuccessHandler — redirects users to their role-specific dashboard.
 *
 *  SUPER_ADMIN → /superadmin/dashboard   (platform overview)
 *  ADMIN       → /admin/dashboard        (clinic-level dashboard)
 *  DOCTOR      → /doctor/dashboard
 *  PATIENT     → /patient/dashboard
 */
public class RoleBasedLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))) {
            response.sendRedirect("/superadmin/dashboard");
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            response.sendRedirect("/admin/dashboard");
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"))) {
            response.sendRedirect("/doctor/dashboard");
        } else {
            response.sendRedirect("/patient/dashboard");
        }
    }
}
