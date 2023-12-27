package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "administrator")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdministratorEntity extends PersonEntity {

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password; // Consider using encryption for storing passwords

    // Additional attributes and relationships specific to an administrator

    // Constructors, getters, setters, and other methods...
}
