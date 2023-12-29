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
    private String groupType;
    private Long levelId;
    private Long subjectId;
    private Integer packageCount;
    private Integer sessionCount;
    private Double sessionPrice;
    private Long teacherId;
    private Set<Student> students;

}
