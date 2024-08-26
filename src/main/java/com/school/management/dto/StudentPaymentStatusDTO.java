package com.school.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPaymentStatusDTO {
    private Long studentId;

    private boolean paymentOverdue;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    private Date dateOfBirth;

    private String placeOfBirth;

    private byte[] photo;

    @NotNull(message = "Level is required")
    private Long level;

    // Getters and setters
}

