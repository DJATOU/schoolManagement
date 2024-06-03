package com.school.management.controller;

import com.school.management.dto.session.SessionDTO;
import com.school.management.dto.session.SessionSearchCriteriaDTO;
import com.school.management.mapper.SessionMapper;
import com.school.management.persistance.SessionEntity;
import com.school.management.service.SessionService;
import com.school.management.service.exception.CustomServiceException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    private final SessionService sessionService;



    private final SessionMapper sessionMapper;

    @Autowired
    public SessionController(SessionService sessionService, SessionMapper sessionMapper){
        this.sessionService = sessionService;
        this.sessionMapper = sessionMapper;
    }

    @GetMapping
    public ResponseEntity<List<SessionDTO>> getAllSessions() {
        logger.info("Getting all sessions");
        List<SessionEntity> sessions = sessionService.getAllSessions();
        List<SessionDTO> sessionDTOs = sessions.stream()
                .map(sessionMapper::sessionEntityToSessionDto)
                .toList();
        return ResponseEntity.ok(sessionDTOs);
    }

    @GetMapping("/detail")
    public ResponseEntity<List<SessionDTO>> getAllSessionsWithDetail() {
        logger.info("Getting all sessions");
        List<SessionEntity> sessions = sessionService.getAllSessionsWithDetail();
        List<SessionDTO> sessionDTOs = sessions.stream()
                .map(sessionMapper::sessionEntityToSessionDto)
                .toList();
        return ResponseEntity.ok(sessionDTOs);
    }


    @GetMapping("/{id}")
    public ResponseEntity<SessionDTO> getSessionById(@PathVariable Long id) {
        SessionEntity session = sessionService.getSessionById(id)
                .orElseThrow(() -> new CustomServiceException("Session not found for ID: " + id));
        SessionDTO sessionDTO = sessionMapper.sessionEntityToSessionDto(session);
        return ResponseEntity.ok(sessionDTO);
    }

    @PostMapping
    public ResponseEntity<SessionDTO> createSession(@Valid @RequestBody SessionDTO sessionDTO) {
        SessionEntity sessionEntity = sessionMapper.sessionDtoToSessionEntity(sessionDTO);
        SessionEntity createdSession = sessionService.createSession(sessionEntity);
        SessionDTO createdSessionDTO = sessionMapper.sessionEntityToSessionDto(createdSession);
        return ResponseEntity.ok(createdSessionDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SessionDTO> patchSession(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        SessionEntity updatedSession = sessionService.updateSession(id, updates);
        SessionDTO sessionDTO = sessionMapper.sessionEntityToSessionDto(updatedSession);
        return ResponseEntity.ok(sessionDTO);
    }


    @GetMapping("/search")
    public ResponseEntity<List<SessionDTO>> searchSessions(@RequestBody SessionSearchCriteriaDTO criteria) {
        List<SessionDTO> sessionDTOs = sessionService.findSessionsByCriteria(criteria)
                .stream()
                .map(sessionMapper::sessionEntityToSessionDto)
                .toList();
        return ResponseEntity.ok(sessionDTOs);
    }

    @PatchMapping("/{sessionId}/finish")
    public ResponseEntity<SessionDTO> markSessionAsFinished(@PathVariable Long sessionId) {
        SessionEntity updatedSession = sessionService.markSessionAsFinished(sessionId);
        return ResponseEntity.ok(sessionMapper.sessionEntityToSessionDto(updatedSession));
    }

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<List<SessionDTO>> getSessionsBySeriesId(@PathVariable Long seriesId) {
        List<SessionDTO> sessions = sessionService.getSessionsBySeriesId(seriesId)
                .stream()
                .map(sessionMapper::sessionEntityToSessionDto)
                .toList();
        return ResponseEntity.ok(sessions);
    }


    @GetMapping("/range")
    public ResponseEntity<List<SessionDTO>> getSessionsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<SessionDTO> sessions = sessionService.findSessionsInRange(start, end);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionDTO>> getSessionsInDateRange(
            @RequestParam Long groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<SessionEntity> sessions = sessionService.getSessionsByGroupIdAndDateRange(groupId, start, end);
        List<SessionDTO> sessionDTOs = sessions.stream()
                .map(sessionMapper::sessionEntityToSessionDto)
                .toList();
        return ResponseEntity.ok(sessionDTOs);
    }


}
