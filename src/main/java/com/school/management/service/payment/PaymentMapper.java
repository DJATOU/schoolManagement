package com.school.management.service.payment;

import com.school.management.dto.PaymentDTO;
import com.school.management.dto.PaymentDetailDTO;
import com.school.management.persistance.PaymentEntity;
import com.school.management.persistance.PaymentDetailEntity;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper centralisé pour les conversions entre entités Payment et DTOs.
 * Utilise MapStruct pour des conversions efficaces et type-safe.
 */
@Mapper(
    componentModel = "spring",
    builder = @Builder(),
    uses = {PaymentCalculationService.class}
)
public abstract class PaymentMapper {

    @Autowired
    protected PaymentCalculationService calculationService;

    // ===========================================
    // Payment Entity ↔ DTO Mappings
    // ===========================================

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "sessionSeries.id", target = "sessionSeriesId")
    @Mapping(source = "session.id", target = "sessionId")
    @Mapping(source = "amountPaid", target = "amountPaid")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "paymentMethod", target = "paymentMethod")
    @Mapping(source = "description", target = "paymentDescription")
    @Mapping(target = "totalSeriesCost", expression = "java(calculationService.calculateTotalSeriesCostForPayment(payment))")
    @Mapping(target = "totalPaidForSeries", expression = "java(calculationService.calculateTotalPaidForSeries(payment))")
    @Mapping(target = "amountOwed", expression = "java(calculationService.calculateAmountOwed(payment))")
    public abstract PaymentDTO toDTO(PaymentEntity payment);

    /**
     * Convertit une liste d'entités Payment en liste de DTOs
     */
    public abstract List<PaymentDTO> toDTOList(List<PaymentEntity> payments);

    // ===========================================
    // PaymentDetail Entity ↔ DTO Mappings
    // ===========================================

    @Mapping(source = "id", target = "paymentDetailId")
    @Mapping(source = "session.id", target = "sessionId")
    @Mapping(source = "session.title", target = "sessionName")
    @Mapping(source = "amountPaid", target = "amountPaid")
    @Mapping(target = "remainingBalance", expression = "java(calculateRemainingBalance(detail))")
    public abstract PaymentDetailDTO toDetailDTO(PaymentDetailEntity detail);

    /**
     * Convertit une liste d'entités PaymentDetail en liste de DTOs
     */
    public abstract List<PaymentDetailDTO> toDetailDTOList(List<PaymentDetailEntity> details);

    // ===========================================
    // Custom Mapping Methods
    // ===========================================

    /**
     * Calcule le solde restant pour un détail de paiement
     */
    protected double calculateRemainingBalance(PaymentDetailEntity detail) {
        if (detail.getSession() == null || 
            detail.getSession().getGroup() == null || 
            detail.getSession().getGroup().getPrice() == null) {
            return 0.0;
        }
        
        double sessionCost = detail.getSession().getGroup().getPrice().getPrice();
        return Math.max(0, sessionCost - detail.getAmountPaid());
    }

    /**
     * Mapping conditionnel pour le type de paiement
     */
    @Named("mapPaymentType")
    protected String mapPaymentType(PaymentEntity payment) {
        if (payment.getSession() != null && payment.getSessionSeries() == null) {
            return "CATCH_UP"; // Paiement de rattrapage
        } else if (payment.getSessionSeries() != null) {
            return "SERIES"; // Paiement de série
        }
        return "UNKNOWN";
    }

    /**
     * Mapping conditionnel pour le statut de paiement enrichi
     */
    @Named("mapEnrichedStatus")
    protected String mapEnrichedStatus(PaymentEntity payment) {
        String baseStatus = payment.getStatus();
        
        if ("completed".equalsIgnoreCase(baseStatus)) {
            return "COMPLETED";
        } else if ("In Progress".equalsIgnoreCase(baseStatus)) {
            double totalCost = calculationService.calculateTotalSeriesCostForPayment(payment);
            double percentagePaid = calculationService.calculatePaymentPercentage(
                payment.getAmountPaid(), totalCost
            );
            
            if (percentagePaid >= 75) {
                return "NEARLY_COMPLETE";
            } else if (percentagePaid >= 25) {
                return "PARTIAL";
            } else {
                return "MINIMAL";
            }
        }
        
        return "UNKNOWN";
    }
}

/**
 * DTO enrichi pour les rapports de paiement
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReportDTO {
    private Long paymentId;
    private Long studentId;
    private String studentName;
    private Long groupId;
    private String groupName;
    private String paymentType; // SERIES, CATCH_UP
    private String enrichedStatus; // COMPLETED, NEARLY_COMPLETE, PARTIAL, MINIMAL
    private Double amountPaid;
    private Double totalCost;
    private Double percentagePaid;
    private LocalDateTime paymentDate;
    private Integer sessionsCount;
    private Integer paidSessionsCount;
}

/**
 * Extension du mapper pour les rapports
 */
@Mapper(
    componentModel = "spring",
    builder = @Builder(),
    uses = {PaymentCalculationService.class}
)
public abstract class PaymentReportMapper {

    @Autowired
    protected PaymentCalculationService calculationService;

    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "student.id", target = "studentId")
    @Mapping(expression = "java(payment.getStudent().getFirstName() + \" \" + payment.getStudent().getLastName())", target = "studentName")
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(target = "paymentType", qualifiedByName = "mapPaymentType")
    @Mapping(target = "enrichedStatus", qualifiedByName = "mapEnrichedStatus")
    @Mapping(source = "amountPaid", target = "amountPaid")
    @Mapping(target = "totalCost", expression = "java(calculationService.calculateTotalSeriesCostForPayment(payment))")
    @Mapping(target = "percentagePaid", expression = "java(calculatePercentagePaid(payment))")
    @Mapping(source = "dateCreation", target = "paymentDate")
    @Mapping(target = "sessionsCount", expression = "java(getSessionsCount(payment))")
    @Mapping(target = "paidSessionsCount", expression = "java(getPaidSessionsCount(payment))")
    public abstract PaymentReportDTO toReportDTO(PaymentEntity payment);

    public abstract List<PaymentReportDTO> toReportDTOList(List<PaymentEntity> payments);

    // Méthodes helper pour les mappings
    protected double calculatePercentagePaid(PaymentEntity payment) {
        double totalCost = calculationService.calculateTotalSeriesCostForPayment(payment);
        return calculationService.calculatePaymentPercentage(payment.getAmountPaid(), totalCost);
    }

    protected Integer getSessionsCount(PaymentEntity payment) {
        if (payment.getSessionSeries() != null && payment.getSessionSeries().getSessions() != null) {
            return payment.getSessionSeries().getSessions().size();
        }
        return payment.getSession() != null ? 1 : 0;
    }

    protected Integer getPaidSessionsCount(PaymentEntity payment) {
        // Cette logique pourrait être plus complexe en fonction de vos besoins
        // Pour l'instant, on retourne une valeur simplifiée
        return payment.getStatus().equalsIgnoreCase("completed") ? getSessionsCount(payment) : 0;
    }

    @Named("mapPaymentType")
    protected String mapPaymentType(PaymentEntity payment) {
        if (payment.getSession() != null && payment.getSessionSeries() == null) {
            return "CATCH_UP";
        } else if (payment.getSessionSeries() != null) {
            return "SERIES";
        }
        return "UNKNOWN";
    }

    @Named("mapEnrichedStatus")
    protected String mapEnrichedStatus(PaymentEntity payment) {
        String baseStatus = payment.getStatus();
        
        if ("completed".equalsIgnoreCase(baseStatus)) {
            return "COMPLETED";
        } else if ("In Progress".equalsIgnoreCase(baseStatus)) {
            double percentagePaid = calculatePercentagePaid(payment);
            
            if (percentagePaid >= 75) {
                return "NEARLY_COMPLETE";
            } else if (percentagePaid >= 25) {
                return "PARTIAL";
            } else {
                return "MINIMAL";
            }
        }
        
        return "UNKNOWN";
    }
}