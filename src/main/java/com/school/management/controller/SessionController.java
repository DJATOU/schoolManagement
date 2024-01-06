package com.school.management.controller;

import com.school.management.persistance.SessionEntity;
import com.school.management.service.PatchService;
import com.school.management.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    private final PatchService patchService;

    @Autowired
    public SessionController(SessionService sessionService, PatchService patchService) {
        this.sessionService = sessionService;
        this.patchService = patchService;
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
    public ResponseEntity<SessionEntity> updateSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.updateSession(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SessionEntity> patchSession(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Optional<SessionEntity> session = sessionService.getSessionById(id);
        patchService.applyPatch(session, updates);
        return ResponseEntity.ok(sessionService.updateSession(id));
    }

    // Additional endpoints as needed...
}
