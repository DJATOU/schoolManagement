package com.school.management.controller;

import com.school.management.persistance.SessionEntity;
import com.school.management.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    @Autowired
    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<List<SessionEntity>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionEntity> getSessionById(@PathVariable Long id) {
        SessionEntity session = sessionService.getSessionById(id)
                .orElseThrow(() -> new RuntimeException("Session not found")); // Customize this exception
        return ResponseEntity.ok(session);
    }

    @PostMapping
    public ResponseEntity<SessionEntity> createSession(@RequestBody SessionEntity session) {
        return ResponseEntity.ok(sessionService.createSession(session));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionEntity> updateSession(@PathVariable Long id, @RequestBody SessionEntity session) {
        return ResponseEntity.ok(sessionService.updateSession(id, session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    // Additional endpoints as needed...
}
