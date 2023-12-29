package com.school.management.controller;

import com.school.management.persistance.GroupTypeEntity;
import com.school.management.repository.GroupTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/grouptypes")
public class GroupTypeController {

    private final GroupTypeRepository groupTypeRepository;

    @Autowired
    public GroupTypeController(GroupTypeRepository groupTypeRepository) {
        this.groupTypeRepository = groupTypeRepository;
    }

    // Get all group types
    @GetMapping
    public ResponseEntity<List<GroupTypeEntity>> getAllGroupTypes() {
        return ResponseEntity.ok(groupTypeRepository.findAll());
    }

    // Get a single group type by ID
    @GetMapping("/{id}")
    public ResponseEntity<GroupTypeEntity> getGroupTypeById(@PathVariable Long id) {
        return groupTypeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Create a new group type
    @PostMapping
    public ResponseEntity<GroupTypeEntity> createGroupType(@Valid @RequestBody GroupTypeEntity groupType) {
        GroupTypeEntity savedGroupType = groupTypeRepository.save(groupType);
        return new ResponseEntity<>(savedGroupType, HttpStatus.CREATED);
    }

    // Update an existing group type
    @PutMapping("/{id}")
    public ResponseEntity<GroupTypeEntity> updateGroupType(@PathVariable Long id, @Valid @RequestBody GroupTypeEntity groupTypeDetails) {
        return groupTypeRepository.findById(id)
                .map(groupType -> {
                    groupType.setName(groupTypeDetails.getName());
                    groupType.setSize(groupTypeDetails.getSize());
                    GroupTypeEntity updatedGroupType = groupTypeRepository.save(groupType);
                    return new ResponseEntity<>(updatedGroupType, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Delete a group type
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteGroupType(@PathVariable Long id) {
        return groupTypeRepository.findById(id)
                .map(groupType -> {
                    groupTypeRepository.delete(groupType);
                    return new ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
