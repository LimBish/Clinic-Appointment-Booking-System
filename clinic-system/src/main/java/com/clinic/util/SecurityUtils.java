package com.clinic.util;

import com.clinic.model.entity.User;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityUtils — convenience methods for getting the currently authenticated user.
 *
 * Used by controllers to pass the logged-in user's ID to service methods
 * without needing to inject UserRepository everywhere.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private static UserRepository userRepository;

    public SecurityUtils(UserRepository repo) {
        SecurityUtils.userRepository = repo;
    }

    /** Returns the ID of the currently logged-in user. */
    public static Long getCurrentUserId() {
        String email = getCurrentEmail();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB"));
    }

    /** Returns the email (username) of the currently logged-in user. */
    public static String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        return auth.getName();
    }
}
