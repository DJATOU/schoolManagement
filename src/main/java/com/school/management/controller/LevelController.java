package com.school.management.controller;

import com.school.management.persistance.LevelEntity;
import com.school.management.service.LevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/levels")
public class LevelController {

    private final LevelService levelService;

    @Autowired
    public LevelController(LevelService levelService) {
        this.levelService = levelService;
    }

    @GetMapping
    public ResponseEntity<List<LevelEntity>> getAllLevels() {

        return ResponseEntity.ok(levelService.getAllLevels());

    }

    @PostMapping
    public ResponseEntity<LevelEntity> createLevel(@RequestBody LevelEntity level) {
        return ResponseEntity.ok(levelService.createLevel(level));
    }

    //uodate level
    @PutMapping("/{id}")
    public ResponseEntity<LevelEntity> updateLevel(@PathVariable Long id, @RequestBody LevelEntity level) {
        return ResponseEntity.ok(levelService.updateLevel(id, level));
    }

}
