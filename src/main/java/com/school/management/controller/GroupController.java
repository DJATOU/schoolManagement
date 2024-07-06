package com.school.management.controller;

import com.school.management.dto.GroupDTO;
import com.school.management.mapper.GroupMapper;
import com.school.management.persistance.GroupEntity;
import com.school.management.service.GroupServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupServiceImpl groupService;
    private final GroupMapper groupMapper;

    private List<GroupDTO> convertToGroupDTOList(List<GroupEntity> groups) {
        return groups.stream()
                .map(groupMapper::groupToGroupDTO)
                .toList();
    }


    @Autowired
    public GroupController(GroupServiceImpl groupService, GroupMapper groupMapper) {
        this.groupService = groupService;
        this.groupMapper = groupMapper;
    }

    @PostMapping("/createGroupe")
    public ResponseEntity<GroupDTO> createGroup(@Valid @ModelAttribute GroupDTO groupDto) {
        GroupEntity group = groupMapper.groupDTOToGroup(groupDto);
        GroupEntity savedGroup = groupService.save(group);
        return new ResponseEntity<>(groupMapper.groupToGroupDTO(savedGroup), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDTO> getGroupById(@PathVariable Long id) {
        GroupEntity group = groupService.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found")); // Customize exception
        return ResponseEntity.ok(groupMapper.groupToGroupDTO(group));
    }

    @GetMapping
    public ResponseEntity<List<GroupDTO>> getAllGroups() {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(groupMapper::groupToGroupDTO)
                .toList();
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupDTO> updateGroup(@PathVariable Long id, @RequestBody GroupDTO groupDto) {
        GroupEntity updatedGroup = groupMapper.groupDTOToGroup(groupDto);
        updatedGroup.setId(id);
        GroupEntity savedGroup = groupService.save(updatedGroup);
        return ResponseEntity.ok(groupMapper.groupToGroupDTO(savedGroup));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teacher/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByTeacherId(@PathVariable Long id) {
        List<GroupDTO> groupDtos = convertToGroupDTOList(groupService.findByTeacherId(id));
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/student/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByStudentId(@PathVariable Long id) {
        List<GroupDTO> groupDtos = convertToGroupDTOList(groupService.findByStudentId(id));
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/subject/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsBySubjectId(@PathVariable Long id) {
        List<GroupEntity> groups = groupService.findAll().stream()
                .filter(group -> group.getSubject().getId().equals(id))
                .toList();
        List<GroupDTO> groupDtos = groups.stream()
                .map(groupMapper::groupToGroupDTO)
                .toList();
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/level/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByLevelId(@PathVariable Long id) {
        List<GroupEntity> groups = groupService.findAll().stream()
                .filter(group -> group.getLevel().getId().equals(id))
                .toList();
        List<GroupDTO> groupDtos = groups.stream()
                .map(groupMapper::groupToGroupDTO)
                .toList();
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/grouptype/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByGroupTypeId(@PathVariable Long id) {
        List<GroupEntity> groups = groupService.findAll().stream()
                .filter(group -> group.getGroupType().getId().equals(id))
                .toList();
        List<GroupDTO> groupDtos = groups.stream()
                .map(groupMapper::groupToGroupDTO)
                .toList();
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/price/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByPriceId(@PathVariable Long id) {
        List<GroupEntity> groups = groupService.findAll().stream()
                .filter(group -> group.getPrice().getId().equals(id))
                .toList();
        List<GroupDTO> groupDtos = groups.stream()
                .map(groupMapper::groupToGroupDTO)
                .toList();
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/searchByNames")
    @Transactional(readOnly = true)
    public ResponseEntity<List<GroupDTO>> getGroupsByName(@RequestParam(required = false) String search) {
        System.out.println("Received search parameter: " + search);
        List<GroupDTO> groups = groupService.searchGroupsByNameStartingWithDTO(search);
        return ResponseEntity.ok(groups);
    }

}
