package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEntity extends PersonEntity {

    @Column(name = "level")
    private String level;

    @ManyToMany
    @JoinTable(
            name = "student_groups",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<GroupEntity> groups = new HashSet<>();

    @OneToMany(mappedBy = "student")
    private Set<AttendanceEntity> attendances = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private TutorEntity tutor; // Adding a reference to the TutorEntity


    @Column(name = "establishment")
    private String establishment;

    @Column(name = "average_score")
    private Double averageScore; // Optional

    // Additional methods as needed...
}
