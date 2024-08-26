package com.school.management.persistance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentEntity extends PersonEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private LevelEntity level; // Reference to LevelEntity

    @ManyToMany
    @JoinTable(
            name = "student_groups",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @JsonIgnoreProperties("students")
    private Set<GroupEntity> groups ;

    @OneToMany(mappedBy = "student")
    private Set<AttendanceEntity> attendances;

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private TutorEntity tutor; // Adding a reference to the TutorEntity


    @Column(name = "establishment")
    private String establishment;

    @Column(name = "average_score")
    @Min(0)
    @Max(100)
    private Double averageScore;
}
