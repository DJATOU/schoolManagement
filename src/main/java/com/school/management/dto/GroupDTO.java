package com.school.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDTO {

    private Long id;

    @NotNull(message = "Group name is required")
    private String name;

    @NotNull(message = "Group type ID is required")
    private Long groupTypeId;

    @NotNull(message = "Level ID is required")
    private Long levelId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @Min(1)
    private int sessionsPerWeek;

    @NotNull(message = "Price ID is required")
    private Long priceId;

    private Date dateCreation;
    private Date dateUpdate;
    private Boolean active;

    private String description;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    private Set<Long> studentIds; // IDs of students in the group

}
