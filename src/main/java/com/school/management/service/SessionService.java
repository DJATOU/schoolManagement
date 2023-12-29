package com.school.management.service;

import com.school.management.persistance.SessionEntity;
import com.school.management.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    @Autowired
    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public List<SessionEntity> getAllSessions() {
        return sessionRepository.findAll();
    }

    public Optional<SessionEntity> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    public SessionEntity createSession(SessionEntity session) {
        return sessionRepository.save(session);
    }

    public SessionEntity updateSession(Long id, SessionEntity sessionDetails) {
        SessionEntity session = getSessionById(id)
                .orElseThrow(() -> new RuntimeException("Session not found")); // Customize this exception
        // Update session fields here
        // ...
        return sessionRepository.save(session);
    }

    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }


}
