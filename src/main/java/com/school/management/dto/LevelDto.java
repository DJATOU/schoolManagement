package com.school.management.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.school.management.persistance.LevelEntity}
 */
@Value
public class LevelDto implements Serializable {
    LocalDateTime dateCreation;
    LocalDateTime dateUpdate;
    String createdBy;
    String updatedBy;
    Boolean active;
    String description;
    Long id;
    String name;
    String levelCode;
}