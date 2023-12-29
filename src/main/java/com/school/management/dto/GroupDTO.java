package com.school.management.dto;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDTO {

    private Long id;

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

    private String priceDescription; // Description of the price

    private BigDecimal priceAmount;

    private Date dateCreation;
    private Date dateUpdate;
    private Boolean active;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    private Set<Long> studentIds; // IDs of students in the group

}
