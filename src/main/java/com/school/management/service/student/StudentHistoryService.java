// Corrected StudentHistoryService.java
package com.school.management.service.student;

import com.school.management.dto.group.GroupHistoryDTO;
import com.school.management.dto.serie.SeriesHistoryDTO;
import com.school.management.dto.session.SessionHistoryDTO;
import com.school.management.dto.student.StudentFullHistoryDTO;
import com.school.management.persistance.*;
import com.school.management.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentHistoryService {

    @Autowired
    private StudentRepository studentRepository;

    public StudentFullHistoryDTO getStudentFullHistory(Long studentId) {
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Étudiant non trouvé"));

        return mapStudentEntityToDTO(student);
    }

    private StudentFullHistoryDTO mapStudentEntityToDTO(StudentEntity student) {
        StudentFullHistoryDTO dto = new StudentFullHistoryDTO();
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getFirstName() + " " + student.getLastName());

        List<GroupHistoryDTO> groupDTOs = student.getGroups().stream()
                .map(group -> mapGroupEntityToDTO(group, student))
                .collect(Collectors.toList());

        dto.setGroups(groupDTOs);

        return dto;
    }

    private GroupHistoryDTO mapGroupEntityToDTO(GroupEntity group, StudentEntity student) {
        GroupHistoryDTO dto = new GroupHistoryDTO();
        dto.setGroupId(group.getId());
        dto.setGroupName(group.getName());

        List<SeriesHistoryDTO> seriesDTOs = group.getSeries().stream()
                .map(series -> mapSeriesEntityToDTO(series, student, group))
                .collect(Collectors.toList());

        dto.setSeries(seriesDTOs);

        return dto;
    }

    private SeriesHistoryDTO mapSeriesEntityToDTO(SessionSeriesEntity series, StudentEntity student, GroupEntity group) {
        SeriesHistoryDTO dto = new SeriesHistoryDTO();
        dto.setSeriesId(series.getId());
        dto.setSeriesName(series.getName());

        double totalPaidForSeries = calculateTotalPaidForSeries(series, student);
        double totalCostOfSeries = calculateTotalCostOfSeries(series, group);

        if (totalPaidForSeries >= totalCostOfSeries) {
            dto.setPaymentStatus("Complet");
        } else {
            dto.setPaymentStatus("Partiel");
        }

        dto.setTotalAmountPaid(totalPaidForSeries);
        dto.setTotalCost(totalCostOfSeries);

        List<SessionEntity> sortedSessions = series.getSessions().stream()
                .sorted(Comparator.comparing(SessionEntity::getSessionTimeStart))
                .collect(Collectors.toList());

        List<SessionHistoryDTO> sessionDTOs = sortedSessions.stream()
                .map(session -> mapSessionEntityToDTO(session, student))
                .collect(Collectors.toList());

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

    private double calculateTotalCostOfSeries(SessionSeriesEntity series, GroupEntity group) {
        double pricePerSession = group.getPrice().getPrice();
        int sessionNumberPerSerie = group.getSessionNumberPerSerie();
        return pricePerSession * sessionNumberPerSerie;
    }

    private SessionHistoryDTO mapSessionEntityToDTO(SessionEntity session, StudentEntity student) {
        SessionHistoryDTO dto = new SessionHistoryDTO();
        dto.setSessionId(session.getId());
        dto.setSessionName(session.getTitle());
        dto.setSessionDate(session.getSessionTimeStart());

        AttendanceEntity attendance = session.getAttendances().stream()
                .filter(a -> a.getStudent().getId().equals(student.getId()))
                .findFirst()
                .orElse(null);

        if (attendance != null) {
            dto.setAttendanceStatus(Boolean.TRUE.equals(attendance.getIsPresent()) ? "Présent" : "Absent");
            dto.setIsJustified(attendance.getIsJustified());
            dto.setDescription(attendance.getDescription());
        } else {
            dto.setAttendanceStatus("Non renseigné");
        }

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
