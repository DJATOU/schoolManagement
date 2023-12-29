package com.school.management.service;

import com.school.management.persistance.SubjectEntity;
import com.school.management.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Autowired
    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<SubjectEntity> getAllSubjects() {
        return subjectRepository.findAll();
    }

    // create a new subject
    public SubjectEntity createSubject(SubjectEntity subject) {
        return subjectRepository.save(subject);
    }

    public SubjectEntity updateSubject(Long id, SubjectEntity subject) {
        SubjectEntity subjectToUpdate = subjectRepository.findById(id).orElseThrow();
        subjectToUpdate.setName(subject.getName());
        return subjectRepository.save(subjectToUpdate);
    }

}
