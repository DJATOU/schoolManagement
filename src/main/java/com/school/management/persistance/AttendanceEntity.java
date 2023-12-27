package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private StudentEntity student; // Reference to the student

    @ManyToOne
    @JoinColumn(name = "session_id")
    private SessionEntity session; // Reference to the session

    @Column(name = "status")
    private Boolean isPresent; // Status of attendance: true (present), false (absent)

    // Additional fields like remarks, timestamp, etc.

    // Constructors, getters, setters...
}
