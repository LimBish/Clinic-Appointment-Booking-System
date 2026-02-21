package com.clinic.util;

import com.clinic.model.entity.User;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * SecurityUtils — convenience methods for getting the currently authenticated user.
 *
 * Used by controllers to pass the logged-in user's ID to service methods
 * without needing to inject UserRepository everywhere.
 */
@Component
@RequiredArgsConstructor()
public class SecurityUtils {


    private final UserRepository userRepository;


    /** Returns the ID of the currently logged-in user. */
    public Long getCurrentUserId() {
        String email = getCurrentEmail();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent())
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB"));

        return null;
    }

    /** Returns the email (username) of the currently logged-in user. */
    public String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        return auth.getName();
    }
}
