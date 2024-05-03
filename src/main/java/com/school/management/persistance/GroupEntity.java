package com.school.management.persistance;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GroupEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "group_type_id")
    private GroupTypeEntity groupType;

    @ManyToOne
    @JoinColumn(name = "level_id")
    private LevelEntity level;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private SubjectEntity subject;

    @Column(name = "session_per_serie")
    private int sessionNumberPerSerie;

    @ManyToOne
    @JoinColumn(name = "price_id")
    private PricingEntity price; // Link to the PriceEntity

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    @JsonBackReference
    private TeacherEntity teacher;

    @Builder.Default
    @ManyToMany(mappedBy = "groups")
    private Set<StudentEntity> students = new HashSet<>();


}
