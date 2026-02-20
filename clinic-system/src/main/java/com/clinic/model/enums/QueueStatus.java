package com.clinic.model.enums;

/**
 * Status of a patient entry in the live queue.
 *
 * WAITING   — patient checked-in, waiting to be called
 * IN_CONSULT — currently with the doctor
 * DONE      — consultation completed; removed from active queue
 * SKIPPED   — admin skipped this patient (e.g., stepped out)
 */
public enum QueueStatus {
    WAITING,
    IN_CONSULT,
    DONE,
    SKIPPED
}
