package com.school.management.service;

import com.school.management.dto.AttendanceDTO;
import com.school.management.mapper.AttendanceMapper;
import com.school.management.persistance.AttendanceEntity;
import com.school.management.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    @Autowired
    public AttendanceService(AttendanceRepository attendanceRepository, AttendanceMapper attendanceMapper) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
    }

    public List<AttendanceEntity> getAllAttendancesold() {
        return attendanceRepository.findAll();
    }

    public List<AttendanceDTO> getAllAttendances() {
        List<AttendanceEntity> attendances = attendanceRepository.findAll();
        return attendances.stream()
                .map(attendanceMapper::attendanceToAttendanceDTO)
                .toList();
    }

    public AttendanceEntity getAttendanceById(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found")); // Customize this exception
    }

    public AttendanceEntity createAttendance(AttendanceEntity attendance) {
        return attendanceRepository.save(attendance);
    }

    public AttendanceEntity updateAttendance(Long id) {
        AttendanceEntity existingAttendance = getAttendanceById(id);
        // Update properties of existingAttendance using values from updatedAttendance
        // ...
        return attendanceRepository.save(existingAttendance);
    }

    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }

    //Save attendance
    public AttendanceEntity save(AttendanceEntity attendance) {
        return attendanceRepository.save(attendance);
    }

    public List<AttendanceEntity> saveAll(List<AttendanceEntity> attendances) {
        for (AttendanceEntity attendance : attendances) {
            if (attendanceRepository.existsByStudentIdAndSessionIdAndActiveTrue(attendance.getStudent().getId(), attendance.getSession().getId())) {
                throw new IllegalArgumentException("Attendance already exists for student ID " + attendance.getStudent().getId() + " and session ID " + attendance.getSession().getId());
            }
        }
        return attendanceRepository.saveAll(attendances);
    }

    public List<AttendanceDTO> getAttendanceBySessionIdTrueOrFalse(Long sessionId) {
        List<AttendanceEntity> attendances = attendanceRepository.findBySessionId(sessionId);
        return attendances.stream()
                .map(attendanceMapper::attendanceToAttendanceDTO)
                .toList();
    }



    @Transactional
    public void deleteBySessionId(Long sessionId) {
        attendanceRepository.deleteBySessionId(sessionId);
    }

    public void deactivateBySessionId(Long sessionId) {
        List<AttendanceEntity> attendances = attendanceRepository.findBySessionId(sessionId);
        for (AttendanceEntity attendance : attendances) {
            attendance.setActive(false);
        }
        attendanceRepository.saveAll(attendances);
    }

    public List<AttendanceDTO> getAttendanceBySessionId(Long sessionId) {
        List<AttendanceEntity> activeAttendances = attendanceRepository.findBySessionIdAndActiveTrue(sessionId);
        return activeAttendances.stream()
                .map(attendanceMapper::attendanceToAttendanceDTO)
                .toList();
    }

    public List<AttendanceDTO> getAttendanceByStudentAndSeries(Long studentId, Long sessionSeriesId) {
        List<AttendanceEntity> attendanceEntities = attendanceRepository.findByStudentIdAndSessionSeriesIdAndActiveTrue(studentId, sessionSeriesId);
        if (attendanceEntities.isEmpty()) {
            System.out.println("No attendance records found for student ID " + studentId + " and series ID " + sessionSeriesId);
        } else {
            System.out.println("Attendance records found: " + attendanceEntities.size());
        }
        return attendanceEntities.stream()
                .map(attendanceMapper::attendanceToAttendanceDTO)
                .toList();
    }

    // Additional methods as needed...
}
