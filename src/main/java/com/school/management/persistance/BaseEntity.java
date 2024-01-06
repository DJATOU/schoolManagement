package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@MappedSuperclass
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

    @Column(name = "date_creation", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @Column(name = "date_update")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateUpdate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "description")
    private String description;

    @PrePersist
    protected void onCreate() {
        dateCreation = new Date();
        active = true;
        // createdBy should be set based on the current user context
        createdBy = "admin";
    }

    @PreUpdate
    protected void onUpdate() {
        dateUpdate = new Date();
    }
}