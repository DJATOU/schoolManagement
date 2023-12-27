package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTypeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name; // e.g., "Grand", "Moyen", "Petit", "Individuelle"

    @Column(name = "size")
    private int size; // +40, +20, +10 for different groups

}
