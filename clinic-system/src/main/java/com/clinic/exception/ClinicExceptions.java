//package com.clinic.exception;
//
///**
// * Custom exception hierarchy for the clinic system.
// *
// * Clear exception types allow the GlobalExceptionHandler to return
// * meaningful error messages to users via Thymeleaf error pages
// * or flash attributes.
// */
//
//// ─── ResourceNotFoundException ────────────────────────────────────────────────
//class ResourceNotFoundException extends RuntimeException {
//    public ResourceNotFoundException(String resource, Long id) {
//        super(resource + " not found with ID: " + id);
//    }
//}
//
//// ─── AppointmentConflictException ─────────────────────────────────────────────
//// Thrown when BR-001 (double booking prevention) is violated
//class AppointmentConflictException extends RuntimeException {
//    public AppointmentConflictException(String message) {
//        super(message);
//    }
//}
//
//// ─── DuplicateResourceException ───────────────────────────────────────────────
//// Thrown when registering with an already-used email or phone
//class DuplicateResourceException extends RuntimeException {
//    public DuplicateResourceException(String message) {
//        super(message);
//    }
//}
//
//// ─── UnauthorizedAccessException ──────────────────────────────────────────────
//// Thrown when a user tries to modify another user's resource
//class UnauthorizedAccessException extends RuntimeException {
//    public UnauthorizedAccessException(String message) {
//        super(message);
//    }
//}
