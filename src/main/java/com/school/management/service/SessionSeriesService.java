package com.school.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.school.management.persistance.SessionSeriesEntity;
import com.school.management.repository.SessionSeriesRepository;

import java.util.List;

@Service
public class SessionSeriesService {

    private final SessionSeriesRepository sessionSeriesRepository;

    @Autowired
    public SessionSeriesService(SessionSeriesRepository sessionSeriesRepository) {
        this.sessionSeriesRepository = sessionSeriesRepository;
    }

    public List<SessionSeriesEntity> getAllSessionSeries() {
        return sessionSeriesRepository.findAll();
    }

    public SessionSeriesEntity getSessionSeriesById(Long id) {
        return sessionSeriesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session series not found"));
    }

    public SessionSeriesEntity createOrUpdateSessionSeries(SessionSeriesEntity sessionSeries) {
        return sessionSeriesRepository.save(sessionSeries);
    }

    public void deleteSessionSeries(Long id) {
        sessionSeriesRepository.deleteById(id);
    }

    public List<SessionSeriesEntity> getSessionSeriesByStudentId(Long id) {
        return sessionSeriesRepository.findByStudentId(id);
    }

    // Ajoutez d'autres méthodes personnalisées ici selon les besoins
}
