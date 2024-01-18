package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.util.Set;

@Entity
@Table(name = "session_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSeriesEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupEntity group; // Le groupe associé à la série de sessions

    @Column(name = "total_sessions")
    private int totalSessions; // Nombre total de séances dans la série

    @Column(name = "sessions_completed")
    private int sessionsCompleted; // Nombre de séances déjà complétées

    @OneToMany(mappedBy = "sessionSeries")
    private Set<SessionEntity> sessions ;
    // ... autres champs et méthodes ...
}

