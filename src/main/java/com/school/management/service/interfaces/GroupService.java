package com.school.management.service.interfaces;

import com.school.management.dto.GroupDTO;
import com.school.management.persistance.GroupEntity;

import java.util.List;

public interface GroupService {
    List<GroupDTO> searchGroupsByNameStartingWithDTO(String name);
    List<GroupEntity> searchGroupsByNameStartingWith(String input);
    void desactivateGroup(Long id);
}
