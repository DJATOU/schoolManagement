package com.school.management.domain.student;

import com.school.management.domain.group.Group;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Getter
@Setter(AccessLevel.PRIVATE) // Keeping setters private to control state changes through methods
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For frameworks
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Used by the Builder
@Builder
public class Student {

    private Long id;
    private String firstName;
    private String lastName;
    private Date dateOfBirth;
    private String placeOfBirth;
    private String phoneNumber;
    private String email;
    private String tutor; // This could also be a separate Tutor class if more details are needed
    private String level;
    private Set<Group> groups; // Assuming a student can belong to multiple groups
    private String establishment;
    private Double averageScore; // Optional

    // Constructors, Getters, Setters

    // Business methods, e.g., enroll in a group, update average score, etc.
}
