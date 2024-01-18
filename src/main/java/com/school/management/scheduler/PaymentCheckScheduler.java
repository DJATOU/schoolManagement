package com.school.management.scheduler;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentCheckScheduler {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PaymentService paymentService;

    // Exécuter cette méthode une fois par jour à minuit
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkForPaymentDelays() {
        List<StudentEntity> students = studentRepository.findAll();
        for (StudentEntity student : students) {
            // Supposons que vous ayez une méthode pour récupérer tous les groupes/séries associés à un étudiant
            List<GroupEntity> groups = groupRepository.findByStudentId(student.getId());
            for (GroupEntity group : groups) {
                Long seriesId = null ;// Obtenez l'ID de la série associée
                double pricePerSession = group.getPrice().getPrice();
                if (paymentService.isStudentPaymentOverdueForSeries(student.getId(), seriesId, pricePerSession)) {
                    // Envoyer une notification de retard de paiement
                }
            }
        }
    }
}
