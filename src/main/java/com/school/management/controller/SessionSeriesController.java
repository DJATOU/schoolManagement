package com.school.management.controller;

import com.school.management.service.PatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.school.management.persistance.SessionSeriesEntity;
import com.school.management.service.SessionSeriesService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessionSeries")
public class SessionSeriesController {

    private final SessionSeriesService sessionSeriesService;

    private final PatchService patchService;

    @Autowired
    public SessionSeriesController(SessionSeriesService sessionSeriesService, PatchService patchService) {
        this.sessionSeriesService = sessionSeriesService;
        this.patchService = patchService;
    }

    @GetMapping
    public ResponseEntity<List<SessionSeriesEntity>> getAllSessionSeries() {
        return ResponseEntity.ok(sessionSeriesService.getAllSessionSeries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionSeriesEntity> getSessionSeriesById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionSeriesService.getSessionSeriesById(id));
    }

    @PostMapping
    public ResponseEntity<SessionSeriesEntity> createSessionSeries(@RequestBody SessionSeriesEntity sessionSeries) {
        return ResponseEntity.ok(sessionSeriesService.createOrUpdateSessionSeries(sessionSeries));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SessionSeriesEntity> patchSessionSeries(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        SessionSeriesEntity sessionSeries = sessionSeriesService.getSessionSeriesById(id);
        patchService.applyPatch(sessionSeries, updates);
        return ResponseEntity.ok(sessionSeriesService.createOrUpdateSessionSeries(sessionSeries));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSessionSeries(@PathVariable Long id) {
        sessionSeriesService.deleteSessionSeries(id);
        return ResponseEntity.noContent().build();
    }

    // get series by student id
}
