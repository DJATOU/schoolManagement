package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tutor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorEntity extends PersonEntity {

    @Column(name = "relationship")
    private String relationship; // e.g., Parent, Guardian, etc.

    @OneToMany(mappedBy = "tutor")
    private Set<StudentEntity> students = new HashSet<>();

}
