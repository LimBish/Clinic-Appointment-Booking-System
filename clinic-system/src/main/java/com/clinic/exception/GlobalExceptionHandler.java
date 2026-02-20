package com.clinic.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;


/**
 * GlobalExceptionHandler — catches unhandled exceptions across all controllers.
 *
 * Instead of showing a stack trace, users see a friendly error page.
 * All errors are logged for ISO/IEC 27001 audit trail.
 *
 * IMPORTANT: NoResourceFoundException (e.g. missing favicon.ico or unknown static paths)
 * is handled separately so it does NOT fall into handleGeneral(), which would try to
 * render error/500 and cause a second TemplateInputException cascade.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public String handleConflict(AppointmentConflictException ex, Model model) {
        log.warn("Appointment conflict: {}", ex.getMessage());
        model.addAttribute("errorCode", "409");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/conflict";
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public String handleUnauthorized(UnauthorizedAccessException ex, Model model) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorMessage", "You are not authorized to perform this action.");
        return "error/403";
    }

    /**
     * Handles Spring MVC's NoResourceFoundException (e.g. /favicon.ico, unknown static paths).
     * Logged at DEBUG level only — these are noisy browser-generated requests, not app errors.
     *
     * IMPORTANT: use response.setStatus() NOT response.sendError().
     * sendError() triggers Tomcat's error-page forwarding (dispatch to /error), which causes
     * Thymeleaf to attempt rendering the "error" template — and if that template is also missing
     * it produces a second TemplateInputException cascade.
     * setStatus() simply sets the HTTP status code and lets the response complete cleanly.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoStaticResource(NoResourceFoundException ex,
                                       HttpServletResponse response) {
        log.debug("Static resource not found (404): {}", ex.getMessage());
        response.setStatus(HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        return "error/500";
    }
}