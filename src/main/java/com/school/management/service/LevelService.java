package com.school.management.service;

import com.school.management.persistance.LevelEntity;
import com.school.management.repository.LevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LevelService {

    private final LevelRepository levelRepository;

    @Autowired
    public LevelService(LevelRepository levelRepository) {
        this.levelRepository = levelRepository;
    }

    public List<LevelEntity> getAllLevels() {
        return levelRepository.findAll();
    }

    public LevelEntity createLevel(LevelEntity level) {
        return levelRepository.save(level);
    }

    public LevelEntity updateLevel(Long id, LevelEntity level) {
        LevelEntity levelToUpdate = levelRepository.findById(id).orElseThrow();
        levelToUpdate.setName(level.getName());
        return levelRepository.save(levelToUpdate);
    }

}
