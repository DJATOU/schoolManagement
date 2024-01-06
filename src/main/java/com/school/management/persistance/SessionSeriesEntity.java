package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne
    @JoinColumn(name = "student_id")
    private StudentEntity student;

    @Column(name = "total_sessions")
    private int totalSessions; // Nombre total de séances dans la série

    @Column(name = "total_price")
    private Double totalPrice; // Prix total de la série

    @Column(name = "amount_paid")
    private Double amountPaid; // Montant déjà payé

    @Column(name = "balance_due")
    private Double balanceDue; // Solde restant dû

    @Column(name = "sessions_completed")
    private int sessionsCompleted; // Nombre de séances déjà complétées

    // ... autres champs et méthodes ...
}

