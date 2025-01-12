package com.school.management.service.student;

import com.school.management.dto.group.GroupHistoryDTO;
import com.school.management.dto.serie.SeriesHistoryDTO;
import com.school.management.dto.session.SessionHistoryDTO;
import com.school.management.dto.student.StudentFullHistoryDTO;
import com.school.management.persistance.*;
import com.school.management.repository.AttendanceRepository;
import com.school.management.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StudentHistoryService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    public StudentHistoryService(StudentRepository studentRepository, AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public StudentFullHistoryDTO getStudentFullHistory(Long studentId) {
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Étudiant non trouvé"));

        return mapStudentEntityToDTO(student);
    }

    private StudentFullHistoryDTO mapStudentEntityToDTO(StudentEntity student) {
        StudentFullHistoryDTO dto = new StudentFullHistoryDTO();
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getFirstName() + " " + student.getLastName());

        // 1) Groupes fixes
        List<GroupEntity> fixedGroups = new ArrayList<>(student.getGroups());

        // 2) Groupes de rattrapage
        List<GroupEntity> catchUpGroups = attendanceRepository
                .findByStudentIdAndIsCatchUp(student.getId(), true)
                .stream()
                .map(AttendanceEntity::getGroup)
                .distinct()
                .toList();

        // 3) Union
        Set<GroupEntity> unionSet = new HashSet<>(fixedGroups);
        unionSet.addAll(catchUpGroups);

        // 4) Construire la liste de GroupHistoryDTO
        List<GroupHistoryDTO> groupDTOs = unionSet.stream()
                .map(group -> mapGroupEntityToDTO(group, student))
                // Optionnel : trier par ordre alphabétique
                //.sorted(Comparator.comparing(GroupEntity::getName))
                .toList();

        dto.setGroups(groupDTOs);

        return dto;
    }

    private GroupHistoryDTO mapGroupEntityToDTO(GroupEntity group, StudentEntity student) {
        GroupHistoryDTO dto = new GroupHistoryDTO();
        dto.setGroupId(group.getId());
        dto.setGroupName(group.getName());

        // Savoir si l'étudiant est officiellement inscrit à ce group
        boolean isOfficial = student.getGroups().contains(group);

        List<SeriesHistoryDTO> seriesDTOs = group.getSeries().stream()
                .map(series -> mapSeriesEntityToDTO(series, student, group, isOfficial))
                .toList();

        boolean isCatchUp = attendanceRepository
                .existsByGroupIdAndStudentIdAndIsCatchUp(group.getId(), student.getId(), true);
        dto.setCatchUp(isCatchUp);
        dto.setSeries(seriesDTOs);
        return dto;
    }


    private SeriesHistoryDTO mapSeriesEntityToDTO(SessionSeriesEntity series,
                                                  StudentEntity student,
                                                  GroupEntity group,
                                                  boolean isOfficial) {
        SeriesHistoryDTO dto = new SeriesHistoryDTO();
        dto.setSeriesId(series.getId());
        dto.setSeriesName(series.getName());

        double totalPaidForSeries = calculateTotalPaidForSeries(series, student);
        double totalCostOfSeries = calculateTotalCostOfSeries(group);

        dto.setPaymentStatus(totalPaidForSeries >= totalCostOfSeries ? "Complet" : "Partiel");
        dto.setTotalAmountPaid(totalPaidForSeries);
        dto.setTotalCost(totalCostOfSeries);

        List<SessionEntity> allSessions = series.getSessions().stream()
                .sorted(Comparator.comparing(SessionEntity::getSessionTimeStart))
                .toList();

        List<SessionEntity> filteredSessions;
        if (isOfficial) {
            // L'étudiant est inscrit, on garde toutes les sessions
            filteredSessions = allSessions;
        } else {
            // L'étudiant n'est pas inscrit, donc c'est un "rattrapage" =>
            //  on ne garde que les sessions où il a un AttendanceEntity
            filteredSessions = allSessions.stream()
                    .filter(session -> session.getAttendances().stream()
                            .anyMatch(a ->
                                    a.getStudent().getId().equals(student.getId())
                                            && a.isActive()
                            )
                    )
                    .toList();
        }

        List<SessionHistoryDTO> sessionDTOs = filteredSessions.stream()
                .map(session -> mapSessionEntityToDTO(session, student))
                .toList();

        dto.setSessions(sessionDTOs);
        return dto;
    }


    private double calculateTotalPaidForSeries(SessionSeriesEntity series, StudentEntity student) {
        return series.getSessions().stream()
                .flatMap(session -> session.getPaymentDetails().stream())
                .filter(pd -> pd.getPayment().getStudent().getId().equals(student.getId()))
                .mapToDouble(PaymentDetailEntity::getAmountPaid)
                .sum();
    }

    private double calculateTotalCostOfSeries(GroupEntity group) {
        double pricePerSession = group.getPrice().getPrice();
        int sessionNumberPerSerie = group.getSessionNumberPerSerie();
        return pricePerSession * sessionNumberPerSerie;
    }

    private SessionHistoryDTO mapSessionEntityToDTO(SessionEntity session, StudentEntity student) {
        SessionHistoryDTO dto = new SessionHistoryDTO();

        // Si la session n’est plus active (= dévalidée)
        if (Boolean.FALSE.equals(session.getActive())) {
            dto.setSessionId(session.getId());
            dto.setSessionName(session.getTitle());
            // On force la présence à "Non renseigné"
            dto.setAttendanceStatus("Non renseigné");
            dto.setIsJustified(false); // inutile d’avoir “oui/non” dans ce cas
            // Le paiement peut être mis à zéro ou laissé tel quel si tu souhaites l’historique
            dto.setPaymentStatus("Non payé");
            dto.setAmountPaid(0.0);
            return dto;
        }

        // Sinon, la session est valide, on applique la logique habituelle
        dto.setSessionId(session.getId());
        dto.setSessionName(session.getTitle());
        dto.setSessionDate(session.getSessionTimeStart());

        // Gérer l’attendance
        AttendanceEntity attendance = session.getAttendances().stream()
                .filter(a -> a.getStudent().getId().equals(student.getId())).filter(AttendanceEntity::isActive)
                .findFirst()
                .orElse(null);

        if (attendance != null) {
            // Si l'attendance est "inactive", on force "Non renseigné"
            if (Boolean.FALSE.equals(attendance.getActive())) {
                dto.setAttendanceStatus("Non renseigné");
                dto.setIsJustified(false);
            } else {
                // Sinon, on applique la logique Présent / Absent
                dto.setAttendanceStatus(Boolean.TRUE.equals(attendance.getIsPresent()) ? "Présent" : "Absent");
                dto.setIsJustified(attendance.getIsJustified());
            }
        } else {
            dto.setAttendanceStatus("Non renseigné");
        }

        // Gérer le paiement
        PaymentDetailEntity paymentDetail = session.getPaymentDetails().stream()
                .filter(pd -> pd.getPayment().getStudent().getId().equals(student.getId()))
                .findFirst()
                .orElse(null);

        if (paymentDetail != null) {
            dto.setPaymentStatus(paymentDetail.getPayment().getStatus());
            dto.setAmountPaid(paymentDetail.getAmountPaid());
            dto.setPaymentDate(paymentDetail.getPaymentDate());
        } else {
            dto.setPaymentStatus("Non payé");
            dto.setAmountPaid(0.0);
            dto.setPaymentDate(null);
        }

        return dto;
    }

}
