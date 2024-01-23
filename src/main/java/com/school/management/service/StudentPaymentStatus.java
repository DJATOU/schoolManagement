package com.school.management.service;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class StudentPaymentStatus {
    private Long studentId;
    private String studentName;
    private boolean isPaymentOverdue;

    public StudentPaymentStatus(Long studentId, String studentName, boolean isPaymentOverdue) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.isPaymentOverdue = isPaymentOverdue;
    }

}
