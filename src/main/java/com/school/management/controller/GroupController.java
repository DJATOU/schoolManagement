package com.school.management.controller;

import com.school.management.dto.GroupDTO;
import com.school.management.persistance.GroupEntity;
import com.school.management.repository.*;
import com.school.management.service.GroupService;
import com.school.management.service.exception.CustomServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupTypeRepository groupTypeRepository;
    private final LevelRepository levelRepository;
    private final SubjectRepository subjectRepository;
    private final PricingRepository priceRepository;
    private final TeacherRepository teacherRepository;


    @Autowired
    public GroupController(GroupService groupService, GroupTypeRepository groupTypeRepository, LevelRepository levelRepository, SubjectRepository subjectRepository, PricingRepository priceRepository, TeacherRepository teacherRepository) {
        this.groupService = groupService;
        this.groupTypeRepository = groupTypeRepository;
        this.levelRepository = levelRepository;
        this.subjectRepository = subjectRepository;
        this.priceRepository = priceRepository;
        this.teacherRepository = teacherRepository;
    }

    @PostMapping
    public ResponseEntity<GroupDTO> createGroup(@RequestBody GroupDTO groupDto) {
        GroupEntity group = convertToEntity(groupDto);
        GroupEntity savedGroup = groupService.save(group);
        return new ResponseEntity<>(convertToDto(savedGroup), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDTO> getGroupById(@PathVariable Long id) {
        GroupEntity group = groupService.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found")); // Customize exception
        return ResponseEntity.ok(convertToDto(group));
    }

    @GetMapping
    public ResponseEntity<List<GroupDTO>> getAllGroups() {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(this::convertToDto)
                .toList(); // Changé ici
        return ResponseEntity.ok(groups);
    }


    @PutMapping("/{id}")
    public ResponseEntity<GroupDTO> updateGroup(@PathVariable Long id, @RequestBody GroupDTO groupDto) {
        GroupEntity group = convertToEntity(groupDto);
        group.setId(id); // Ensure correct group is updated
        GroupEntity updatedGroup = groupService.save(group);
        return ResponseEntity.ok(convertToDto(updatedGroup));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    //get all groups by teacher id
    @GetMapping("/teacher/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByTeacherId(@PathVariable Long id) {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(this::convertToDto)
                .toList(); // Changé ici
        return ResponseEntity.ok(groups);
    }

    //get all groups by student id
    @GetMapping("/student/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByStudentId(@PathVariable Long id) {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(this::convertToDto)
                .toList(); // Changé ici
        groups = groups.stream()
                .filter(group -> group.getStudentIds().contains(id))
                .collect(Collectors.toList());
        return ResponseEntity.ok(groups);
    }

    //get all groups by subject id
    @GetMapping("/subject/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsBySubjectId(@PathVariable Long id) {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(this::convertToDto)
                .toList(); // Changé ici
        return ResponseEntity.ok(groups);
    }


    @GetMapping("/level/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByLevelId(@PathVariable Long id) {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(this::convertToDto)
                .toList(); // Changé ici
        return ResponseEntity.ok(groups);
     }

    @GetMapping("/grouptype/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByGroupTypeId(@PathVariable Long id) {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(this::convertToDto)
                .toList(); // Changé ici
        return ResponseEntity.ok(groups);
    }


    @GetMapping("/price/{id}")
    public ResponseEntity<List<GroupDTO>> getAllGroupsByPriceId(@PathVariable Long id) {
        List<GroupDTO> groups = groupService.findAll().stream()
                .map(this::convertToDto)
                .toList(); // Changé ici
        return ResponseEntity.ok(groups);
    }





    // DTO conversion methods
    private GroupDTO convertToDto(GroupEntity group) {
        GroupDTO groupDto = new GroupDTO();
        groupDto.setId(group.getId());
        groupDto.setGroupTypeId(group.getGroupType() != null ? group.getGroupType().getId() : null);
        groupDto.setLevelId(group.getLevel() != null ? group.getLevel().getId() : null);
        groupDto.setSubjectId(group.getSubject() != null ? group.getSubject().getId() : null);
        groupDto.setSessionsPerWeek(group.getSessionsPerWeek());
        groupDto.setPriceId(group.getPrice() != null ? group.getPrice().getId() : null);
        groupDto.setDateCreation(group.getDateCreation());
        groupDto.setDateUpdate(group.getDateUpdate());
        groupDto.setActive(group.getActive());
        groupDto.setTeacherId(group.getTeacher() != null ? group.getTeacher().getId() : null);
        // Set other fields as necessary

        // Set price description and amount
        if (group.getPrice() != null) {
            groupDto.setPriceDescription(group.getPrice().getDescription());
            groupDto.setPriceAmount(group.getPrice().getPrice());
        }

        return groupDto;
    }


    private GroupEntity convertToEntity(GroupDTO groupDto) {
        GroupEntity group = new GroupEntity();

        // Assume repositories for related entities are autowired
        group.setId(groupDto.getId());
        group.setGroupType(groupTypeRepository.findById(groupDto.getGroupTypeId())
                .orElseThrow(() -> new CustomServiceException("Group type not found")));
        group.setLevel(levelRepository.findById(groupDto.getLevelId())
                .orElseThrow(() -> new CustomServiceException("Level not found")));
        group.setSubject(subjectRepository.findById(groupDto.getSubjectId())
                .orElseThrow(() -> new CustomServiceException("Subject not found")));
        group.setSessionsPerWeek(groupDto.getSessionsPerWeek());
        group.setPrice(priceRepository.findById(groupDto.getPriceId())
                .orElseThrow(() -> new CustomServiceException("Pricing not found")));
        group.setActive(groupDto.getActive());
        group.setTeacher(teacherRepository.findById(groupDto.getTeacherId())
                .orElseThrow(() -> new CustomServiceException("Teacher not found")));

        // Set other fields as necessary

        return group;
    }


}
