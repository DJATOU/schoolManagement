package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class PersonEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "date_of_birth")
    @Temporal(TemporalType.DATE) // or TemporalType.TIMESTAMP based on precision needed
    private Date dateOfBirth;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Lob
    @Column(name = "photo")
    private byte[] photo; // Photo as a byte array

    // Additional common fields and methods...
}