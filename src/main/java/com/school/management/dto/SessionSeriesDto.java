package com.school.management.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.school.management.persistance.SessionSeriesEntity}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSeriesDto implements Serializable {
    LocalDateTime dateCreation;
    LocalDateTime dateUpdate;
    String createdBy;
    String updatedBy;
    Boolean active;
    String description;
    Long id;
    int totalSessions;
    Double totalPrice;
    Double amountPaid;
    Double balanceDue;
    int sessionsCompleted;
}