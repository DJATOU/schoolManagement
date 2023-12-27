package com.school.management.persistance;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teacher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherEntity extends PersonEntity {

    // Assuming a teacher can be associated with multiple groups
    @OneToMany(mappedBy = "teacher")
    private Set<GroupEntity> groups = new HashSet<>();

    // Additional fields and methods as needed...
}
