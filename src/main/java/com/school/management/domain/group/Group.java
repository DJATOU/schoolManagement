package com.school.management.domain.group;

import com.school.management.domain.student.Student;
import lombok.*;

import java.util.Set;

@Getter
@Setter(AccessLevel.PRIVATE) // Keeping setters private to control state changes through methods
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For frameworks
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Used by the Builder
@Builder
public class Group {

    private Long id;
    private String groupType; // e.g., Large, Medium, Small, Individual
    private Long levelId; // This might reference another domain object or an enum
    private Long subjectId; // Similar to levelId, could be a domain object or an enum
    private Integer packageCount;
    private Integer sessionCount;
    private Double sessionPrice;
    private Long teacherId; // This could also be a Teacher class if you have teacher details
    private Set<Student> students;

    // Constructors, Getters, Setters

    // Business methods, e.g., add student, calculate group price, etc.
}
