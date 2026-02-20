package com.clinic.model.enums;

/**
 * Lifecycle states of a patient appointment.
 *
 * PENDING    — booked, awaiting confirmation
 * CONFIRMED  — slot locked; reminder will be sent
 * CHECKED_IN — patient has arrived at the clinic; added to queue
 * COMPLETED  — consultation done; doctor marked as finished
 * CANCELLED  — cancelled by patient or admin
 * NO_SHOW    — patient did not arrive (set by scheduler after slot passes)
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
