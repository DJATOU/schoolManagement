package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "level")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name; // "1st", "2nd", "3rd", etc.


}
