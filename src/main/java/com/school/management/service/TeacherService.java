package com.school.management.service;

import com.school.management.persistance.TeacherEntity;
import com.school.management.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Autowired
    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public List<TeacherEntity> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public List<TeacherEntity> findByLastName(String lastName) {
        return teacherRepository.findByLastName(lastName);
    }

    public List<TeacherEntity> findByFirstNameAndLastName(String firstName, String lastName) {
        return teacherRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    public List<TeacherEntity> findByGroupsId(Long groupId) {
        return teacherRepository.findByGroups_Id(groupId);
    }

    public TeacherEntity createTeacher(TeacherEntity teacher) {
        return teacherRepository.save(teacher);
    }

    public TeacherEntity updateTeacher(Long id, TeacherEntity teacher) {
        TeacherEntity teacherToUpdate = teacherRepository.findById(id).orElseThrow();
        teacherToUpdate.setFirstName(teacher.getFirstName());
        teacherToUpdate.setLastName(teacher.getLastName());
        teacherToUpdate.setGroups(teacher.getGroups());
        return teacherRepository.save(teacherToUpdate);
    }

}

