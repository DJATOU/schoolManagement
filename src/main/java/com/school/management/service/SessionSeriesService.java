package com.school.management.service;

import com.school.management.dto.SessionSeriesDto;
import com.school.management.mapper.SessionSeriesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.school.management.persistance.SessionSeriesEntity;
import com.school.management.repository.SessionSeriesRepository;

import java.util.List;

@Service
public class SessionSeriesService {

    private final SessionSeriesRepository sessionSeriesRepository;

    private final SessionSeriesMapper sessionSeriesMapper;

    @Autowired
    public SessionSeriesService(SessionSeriesRepository sessionSeriesRepository, SessionSeriesMapper sessionSeriesMapper) {
        this.sessionSeriesRepository = sessionSeriesRepository;
        this.sessionSeriesMapper = sessionSeriesMapper;
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

    public List<SessionSeriesDto> getSeriesByGroupId(Long groupId) {
        return sessionSeriesRepository.findByGroupId(groupId).stream()
                .map(sessionSeriesMapper::toDto)
                .toList();
    }

    public List<SessionSeriesDto> getSessionSeriesByGroupId(Long groupId) {
        return sessionSeriesRepository.findByGroupId(groupId).stream()
                .map(sessionSeriesMapper::toDto)
                .toList();
    }


    // Ajoutez d'autres méthodes personnalisées ici selon les besoins
}
