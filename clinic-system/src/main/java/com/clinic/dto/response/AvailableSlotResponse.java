package com.clinic.dto.response;

import lombok.*;
import java.time.LocalTime;

/** Represents one time slot in a doctor's day — available or already booked. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailableSlotResponse {
    private LocalTime time;
    private boolean available;
}
