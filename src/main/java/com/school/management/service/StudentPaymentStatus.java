package com.school.management.service;

import com.school.management.dto.StudentDTO;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class StudentPaymentStatus extends StudentDTO {
    private boolean isPaymentOverdue;

    public StudentPaymentStatus(
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            Date dateOfBirth,
            String placeOfBirth,
            String photo,
            String level,
            Set<Long> groupIds,
            Long tutorId,
            String establishment,
            Double averageScore,
            boolean isPaymentOverdue
    ) {
        super(firstName, lastName, email, phoneNumber, dateOfBirth, placeOfBirth, photo, level, groupIds, tutorId, establishment, averageScore);
        this.isPaymentOverdue = isPaymentOverdue;
    }

}
