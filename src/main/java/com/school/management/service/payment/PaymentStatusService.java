package com.school.management.service.payment;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.SessionSeriesEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.SessionSeriesRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.service.GroupPaymentStatus;
import com.school.management.service.SeriesPaymentStatus;
import com.school.management.service.SessionPaymentStatus;
import com.school.management.service.StudentPaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsable de la gestion des statuts de paiement et de l'analyse des retards.
 * Centralise la logique d'évaluation des statuts de paiement pour les étudiants, groupes et séries.
 */
@Service
@Transactional(readOnly = true)
public class PaymentStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentStatusService.class);

    private final PaymentQueryService paymentQueryService;
    private final PaymentCalculationService calculationService;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final SessionSeriesRepository sessionSeriesRepository;

    public PaymentStatusService(
            PaymentQueryService paymentQueryService,
            PaymentCalculationService calculationService,
            StudentRepository studentRepository,
            GroupRepository groupRepository,
            SessionSeriesRepository sessionSeriesRepository
    ) {
        this.paymentQueryService = paymentQueryService;
        this.calculationService = calculationService;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.sessionSeriesRepository = sessionSeriesRepository;
    }

    /**
     * Détermine le statut de paiement pour tous les étudiants d'un groupe
     */
    public List<StudentPaymentStatus> getPaymentStatusForGroup(Long groupId) {
        LOGGER.info("Getting payment status for group {}", groupId);
        
        List<StudentPaymentStatus> result = new ArrayList<>();
        GroupEntity group = getGroupById(groupId);
        List<StudentEntity> students = studentRepository.findByGroups_Id(groupId);

        LOGGER.debug("Found {} students in group {}", students.size(), groupId);

        for (StudentEntity student : students) {
            boolean isOverdue = isStudentPaymentOverdueForGroup(student.getId(), groupId, group.getPrice().getPrice());
            
            StudentPaymentStatus paymentStatus = createStudentPaymentStatus(student, isOverdue);
            result.add(paymentStatus);
        }

        LOGGER.info("Payment status calculated for {} students", result.size());
        return result;
    }

    /**
     * Vérifie si un étudiant a des retards de paiement pour une série donnée
     */
    public boolean isStudentPaymentOverdueForSeries(Long studentId, Long sessionSeriesId, double pricePerSession) {
        LOGGER.debug("Checking payment overdue status for student {} in series {}", studentId, sessionSeriesId);

        long sessionsAttended = paymentQueryService.countAttendedSessionsForSeries(studentId, sessionSeriesId);
        double totalDue = calculationService.calculateAmountDueForAttendedSessions(sessionsAttended, pricePerSession);
        double totalPaid = paymentQueryService.getTotalPaidForStudentAndSeries(studentId, sessionSeriesId);

        boolean isOverdue = totalPaid < totalDue;
        
        LOGGER.debug("Student {} - Sessions attended: {}, Total due: {}, Total paid: {}, Overdue: {}", 
                     studentId, sessionsAttended, totalDue, totalPaid, isOverdue);
        
        return isOverdue;
    }

    /**
     * Vérifie si un étudiant a des retards de paiement pour un groupe (toutes séries confondues)
     */
    public boolean isStudentPaymentOverdueForGroup(Long studentId, Long groupId, double pricePerSession) {
        List<SessionSeriesEntity> seriesList = sessionSeriesRepository.findByGroupId(groupId);
        
        for (SessionSeriesEntity series : seriesList) {
            if (isStudentPaymentOverdueForSeries(studentId, series.getId(), pricePerSession)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Récupère le statut de paiement détaillé pour un étudiant
     */
    public List<GroupPaymentStatus> getPaymentStatusForStudent(Long studentId) {
        LOGGER.info("Getting detailed payment status for student {}", studentId);
        
        List<GroupPaymentStatus> groupStatuses = new ArrayList<>();
        List<GroupEntity> groups = groupRepository.findByStudents_Id(studentId);

        for (GroupEntity group : groups) {
            List<SeriesPaymentStatus> seriesStatuses = new ArrayList<>();
            List<SessionSeriesEntity> seriesList = sessionSeriesRepository.findByGroupId(group.getId());

            for (SessionSeriesEntity series : seriesList) {
                List<SessionPaymentStatus> sessionStatuses = getSessionPaymentStatuses(studentId, series);
                seriesStatuses.add(new SeriesPaymentStatus(series.getId(), sessionStatuses));
            }
            
            groupStatuses.add(new GroupPaymentStatus(group.getId(), group.getName(), seriesStatuses));
        }

        LOGGER.info("Detailed payment status calculated for student {} across {} groups", studentId, groupStatuses.size());
        return groupStatuses;
    }

    /**
     * Vérifie si un paiement est en retard pour une session spécifique
     */
    public boolean isPaymentOverdueForSession(Long studentId, Long sessionId) {
        LOGGER.debug("Checking payment overdue for student {} and session {}", studentId, sessionId);

        var paymentDetails = paymentQueryService.getPaymentDetailsForSession(studentId, sessionId);
        
        // Calculer le montant total payé pour cette session
        double totalPaidForSession = paymentDetails.stream()
                .mapToDouble(detail -> detail.getAmountPaid())
                .sum();

        // Récupérer le coût de la session (on suppose qu'il faut récupérer la session)
        // Pour simplifier, on utilise le premier détail pour obtenir le coût
        if (paymentDetails.isEmpty()) {
            // Aucun paiement = retard si l'étudiant a assisté
            return true; // Logique simplifiée, pourrait être plus complexe
        }

        double sessionCost = paymentDetails.get(0).getSession().getGroup().getPrice().getPrice();
        boolean isOverdue = totalPaidForSession < sessionCost;
        
        LOGGER.debug("Session {} - Cost: {}, Paid: {}, Overdue: {}", 
                     sessionId, sessionCost, totalPaidForSession, isOverdue);
        
        return isOverdue;
    }

    /**
     * Calcule le pourcentage de paiement global d'un étudiant pour un groupe
     */
    public double calculatePaymentPercentageForGroup(Long studentId, Long groupId) {
        GroupEntity group = getGroupById(groupId);
        List<SessionSeriesEntity> seriesList = sessionSeriesRepository.findByGroupId(groupId);
        
        double totalDue = 0.0;
        double totalPaid = 0.0;
        
        for (SessionSeriesEntity series : seriesList) {
            long attendedSessions = paymentQueryService.countAttendedSessionsForSeries(studentId, series.getId());
            double seriesDue = calculationService.calculateAmountDueForAttendedSessions(
                attendedSessions, group.getPrice().getPrice()
            );
            double seriesPaid = paymentQueryService.getTotalPaidForStudentAndSeries(studentId, series.getId());
            
            totalDue += seriesDue;
            totalPaid += seriesPaid;
        }
        
        return calculationService.calculatePaymentPercentage(totalPaid, totalDue);
    }

    /**
     * Identifie les étudiants avec des retards de paiement dans un groupe
     */
    public List<StudentEntity> getOverdueStudentsForGroup(Long groupId) {
        List<StudentEntity> allStudents = studentRepository.findByGroups_Id(groupId);
        GroupEntity group = getGroupById(groupId);
        
        return allStudents.stream()
                .filter(student -> isStudentPaymentOverdueForGroup(
                    student.getId(), groupId, group.getPrice().getPrice()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Calcule le nombre total d'étudiants en retard pour un groupe
     */
    public long countOverdueStudentsForGroup(Long groupId) {
        return getOverdueStudentsForGroup(groupId).size();
    }

    /**
     * Détermine si un paiement de série est complet
     */
    public boolean isSeriesPaymentComplete(Long studentId, Long sessionSeriesId, GroupEntity group) {
        double totalSeriesCost = calculationService.calculateTotalSeriesCost(group);
        double totalPaid = paymentQueryService.getTotalPaidForStudentAndSeries(studentId, sessionSeriesId);
        
        return totalPaid >= totalSeriesCost;
    }

    /**
     * Récupère les statuts de paiement pour toutes les sessions d'une série
     */
    private List<SessionPaymentStatus> getSessionPaymentStatuses(Long studentId, SessionSeriesEntity series) {
        // Cette méthode devrait être implémentée en fonction de vos besoins spécifiques
        // Pour l'instant, retournons une liste vide
        return new ArrayList<>();
    }

    /**
     * Crée un objet StudentPaymentStatus à partir d'un étudiant
     */
    private StudentPaymentStatus createStudentPaymentStatus(StudentEntity student, boolean isOverdue) {
        return new StudentPaymentStatus(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getGender(),
                student.getEmail(),
                student.getPhoneNumber(),
                student.getDateOfBirth(),
                student.getPlaceOfBirth(),
                student.getPhoto(),
                student.getLevel() != null ? student.getLevel().getId() : null,
                student.getGroups().stream().map(GroupEntity::getId).collect(Collectors.toSet()),
                student.getTutor() != null ? student.getTutor().getId() : null,
                student.getEstablishment(),
                student.getAverageScore(),
                student.getActive(),
                isOverdue
        );
    }

    /**
     * Récupère un groupe par son ID avec gestion d'erreur
     */
    private GroupEntity getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with ID: " + groupId));
    }

    /**
     * Génère un rapport de synthèse des paiements pour un groupe
     */
    public PaymentSummaryReport generatePaymentSummaryForGroup(Long groupId) {
        List<StudentEntity> allStudents = studentRepository.findByGroups_Id(groupId);
        List<StudentEntity> overdueStudents = getOverdueStudentsForGroup(groupId);
        
        int totalStudents = allStudents.size();
        int overdueCount = overdueStudents.size();
        double overduePercentage = totalStudents > 0 ? 
            (double) overdueCount / totalStudents * 100 : 0.0;
        
        return new PaymentSummaryReport(totalStudents, overdueCount, overduePercentage);
    }

    /**
     * Classe interne pour le rapport de synthèse
     */
    public static class PaymentSummaryReport {
        private final int totalStudents;
        private final int overdueStudents;
        private final double overduePercentage;

        public PaymentSummaryReport(int totalStudents, int overdueStudents, double overduePercentage) {
            this.totalStudents = totalStudents;
            this.overdueStudents = overdueStudents;
            this.overduePercentage = overduePercentage;
        }

        // Getters
        public int getTotalStudents() { return totalStudents; }
        public int getOverdueStudents() { return overdueStudents; }
        public double getOverduePercentage() { return overduePercentage; }
    }
}