package com.school.management.service;

import com.school.management.persistance.AttendanceEntity;
import com.school.management.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    @Autowired
    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<AttendanceEntity> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    public AttendanceEntity getAttendanceById(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found")); // Customize this exception
    }

    public AttendanceEntity createAttendance(AttendanceEntity attendance) {
        return attendanceRepository.save(attendance);
    }

    public AttendanceEntity updateAttendance(Long id, AttendanceEntity updatedAttendance) {
        AttendanceEntity existingAttendance = getAttendanceById(id);
        // Update properties of existingAttendance using values from updatedAttendance
        // ...
        return attendanceRepository.save(existingAttendance);
    }

    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }

    // Additional methods as needed...
}
