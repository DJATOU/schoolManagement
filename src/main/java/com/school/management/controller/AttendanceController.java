package com.school.management.controller;

import com.school.management.dto.AttendanceDTO;
import com.school.management.mapper.AttendanceMapper;
import com.school.management.persistance.AttendanceEntity;
import com.school.management.service.AttendanceService;
import com.school.management.service.PatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    private final PatchService patchService;

    private final AttendanceMapper attendanceMapper;

    @Autowired
    public AttendanceController(AttendanceService attendanceService, PatchService patchService, AttendanceMapper attendanceMapper){
        this.attendanceService = attendanceService;
        this.patchService = patchService;
        this.attendanceMapper = attendanceMapper;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<List<AttendanceDTO>> getAllAttendances() {
        return ResponseEntity.ok(attendanceService.getAllAttendances());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceEntity> getAttendanceById(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id));
    }

    @PostMapping
    public ResponseEntity<AttendanceDTO> createAttendance(@RequestBody AttendanceDTO attendanceDto) {
        AttendanceEntity attendance = attendanceMapper.attendanceDTOToAttendance(attendanceDto);
        AttendanceEntity savedAttendance = attendanceService.createAttendance(attendance);
        return new ResponseEntity<>(attendanceMapper.attendanceToAttendanceDTO(savedAttendance), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceEntity> updateAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AttendanceDTO> patchAttendance(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        AttendanceEntity attendance = attendanceService.getAttendanceById(id);
        patchService.applyPatch(attendance, updates);
        AttendanceEntity updatedAttendance = attendanceService.save(attendance);
        AttendanceDTO attendanceDTO = attendanceMapper.attendanceToAttendanceDTO(updatedAttendance);
        return ResponseEntity.ok(attendanceDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AttendanceDTO>> submitAttendance(@RequestBody List<AttendanceDTO> attendanceDTOs) {
        List<AttendanceEntity> attendanceEntities = attendanceDTOs.stream()
                .map(attendanceMapper::attendanceDTOToAttendance)
                .toList();

        List<AttendanceEntity> savedAttendances = attendanceService.saveAll(attendanceEntities);
        List<AttendanceDTO> savedAttendanceDTOs = savedAttendances.stream()
                .map(attendanceMapper::attendanceToAttendanceDTO)
                .toList();

        return ResponseEntity.ok(savedAttendanceDTOs);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AttendanceDTO>> getAttendancesBySessionId(@PathVariable Long sessionId) {
        List<AttendanceDTO> attendances = attendanceService.getAttendanceBySessionId(sessionId);
        return ResponseEntity.ok(attendances);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> deleteAttendanceBySessionId(@PathVariable Long sessionId) {
        attendanceService.deleteBySessionId(sessionId);
        return ResponseEntity.noContent().build();
    }


}
