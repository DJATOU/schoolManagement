package com.school.management.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDTO {

    Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @Email(message = "Email should be valid")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    private Date dateOfBirth;

    private String placeOfBirth;

    private String photo;

    @NotNull(message = "Level is required")
    private String level;

    private Set<Long> groupIds; // Assuming groups are identified by their IDs

    private Long tutorId; // Assuming tutor is identified by their ID

    private String establishment;

    @Min(0)
    @Max(100)
    private Double averageScore;


}
