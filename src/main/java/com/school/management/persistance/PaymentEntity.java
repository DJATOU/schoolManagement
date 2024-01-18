package com.school.management.persistance;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student; // L'étudiant qui effectue le paiement

    @ManyToOne
    @JoinColumn(name = "session_id")
    private SessionEntity session;

    @Column(name = "amount_paid", nullable = false)
    private Double amountPaid; // Le montant payé

    @Column(name = "payment_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paymentDate; // La date du paiement

    @Column(name = "payment_for_month")
    private Date paymentForMonth; // Le mois pour lequel le paiement est effectué

    @Column(name = "status")
    private String status; // Par exemple, "Completed", "Pending", "Overdue"

    @Column(name = "payment_method")
    private String paymentMethod;

    @ManyToOne
    @JoinColumn(name = "session_series_id")
    private SessionSeriesEntity sessionSeries;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    @Override
    protected void onCreate() {
        super.onCreate();
        paymentDate = new Date();
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        paymentDate = new Date();
    }

}

