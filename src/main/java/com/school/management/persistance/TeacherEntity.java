package com.school.management.persistance;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "teacher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class TeacherEntity extends PersonEntity {

    // Assuming a teacher can be associated with multiple groups
    @OneToMany(mappedBy = "teacher", fetch = FetchType.EAGER)
    @JsonManagedReference
    private Set<GroupEntity> groups ;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "qualifications", columnDefinition = "TEXT")
    private String qualifications;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "teaching_philosophy", columnDefinition = "TEXT")
    private String teachingPhilosophy;






}
