package com.school.management.service;

import com.school.management.dto.session.SessionDTO;
import com.school.management.dto.session.SessionSearchCriteriaDTO;
import com.school.management.mapper.SessionMapper;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.SessionEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.SessionRepository;
import com.school.management.service.exception.CustomServiceException;
import com.school.management.service.util.CommonSpecifications;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SessionService {

    private static final String SESSION_NOT_FOUND_MESSAGE = "Session not found with id: ";
    private static final String GROUPID = "groupId";
    private final SessionRepository sessionRepository;
    private final GroupRepository groupRepository;
    private final PatchService patchService;
    private final SessionMapper sessionMapper;

    @Autowired
    public SessionService(SessionRepository sessionRepository, PatchService patchService, GroupRepository groupRepository,
                          SessionMapper sessionMapper) {
        this.sessionRepository = sessionRepository;
        this.patchService = patchService;
        this.groupRepository = groupRepository;
        this.sessionMapper = sessionMapper;
    }

    public List<SessionEntity> getAllSessions() {
        return sessionRepository.findAll();
    }

    public List<SessionEntity> getAllSessionsWithDetail() {
        return sessionRepository.findAllWithDetails();
    }

    public Optional<SessionEntity> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    public SessionEntity createSession(SessionEntity session) {
        return sessionRepository.save(session);
    }

    public SessionEntity updateSession(Long sessionId, Map<String, Object> updates) {
        SessionEntity session = getSessionById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(SESSION_NOT_FOUND_MESSAGE + sessionId));

        if (updates.containsKey(GROUPID)) {
            Object groupIdObj = updates.get(GROUPID);

            Long groupId = null;
            if (groupIdObj != null) {
                try {
                    groupId = Long.valueOf(groupIdObj.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid groupId: " + groupIdObj);
                }
            }

            updateGroup(session, groupId);
            updates.remove(GROUPID);
        }


        patchService.applyPatch(session, updates);
        return sessionRepository.save(session);
    }

    private void updateGroup(SessionEntity session, Long groupId) {
        if (groupId != null) {
            GroupEntity group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));
            session.setGroup(group);
        } else {
            session.setGroup(null);
        }
    }

    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }

    public List<SessionEntity> findSessionsByCriteria(SessionSearchCriteriaDTO criteria) {
        Specification<SessionEntity> spec = Specification.where(null);

        spec = spec.and(CommonSpecifications.likeIfNotNull("title", criteria.getTitle()))
                .and(CommonSpecifications.equalsIfNotNull("sessionType", criteria.getSessionType()))
                .and(CommonSpecifications.greaterThanOrEqualToIfNotNull("sessionTimeStart", criteria.getStartDate()))
                .and(CommonSpecifications.lessThanOrEqualToIfNotNull("sessionTimeEnd", criteria.getEndDate()))
                .and(CommonSpecifications.equalsIfNotNull("teacher.id", criteria.getTeacherId()))
                .and(CommonSpecifications.equalsIfNotNull("group.id", criteria.getGroupId()))
                .and(CommonSpecifications.equalsIfNotNull("isFinished", criteria.getIsFinished()))
                .and(CommonSpecifications.equalsIfNotNull("room.id", criteria.getRoomId()));

        return sessionRepository.findAll(spec);
    }

    @Transactional
    public SessionEntity markSessionAsFinished(Long sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomServiceException(SESSION_NOT_FOUND_MESSAGE + sessionId));
        session.setIsFinished(true);
        return sessionRepository.save(session);
    }

    public SessionEntity markSessionAsUnfinished(Long sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomServiceException(SESSION_NOT_FOUND_MESSAGE + sessionId));
        session.setIsFinished(false);
        return sessionRepository.save(session);
    }

    public List<SessionEntity> getSessionsBySeriesId(Long seriesId) {
        return sessionRepository.findBySessionSeriesId(seriesId);
    }

    public List<SessionDTO> findSessionsInRange(LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findBySessionTimeStartBetween(start, end).stream().map(sessionMapper::sessionEntityToSessionDto)
                .toList();
    }

    public List<SessionEntity> getSessionsByGroupIdAndDateRange(Long groupId, LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findByGroupIdAndSessionTimeStartBetween(groupId, start, end);
    }

    public List<SessionDTO> findByGroupIdAndSessionTimeStartBetween(Long groupId, LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findByGroupIdAndSessionTimeStartBetween(groupId, start, end)
                .stream()
                .map(sessionMapper::sessionEntityToSessionDto)
                .toList();
    }


}