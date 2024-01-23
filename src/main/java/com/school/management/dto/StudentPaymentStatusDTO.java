package com.school.management.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPaymentStatusDTO {
    private Long studentId;
    private String studentName;
    private boolean paymentOverdue;

    // Getters and setters
}

