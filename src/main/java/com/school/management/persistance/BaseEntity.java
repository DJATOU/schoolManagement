package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@MappedSuperclass
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public abstract class BaseEntity {

    @Column(name = "date_creation", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @Column(name = "date_update")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateUpdate;

    @Column(name = "active")
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        dateCreation = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        dateUpdate = new Date();
    }
}