package com.school.management.service.interfaces;

import com.school.management.dto.GroupDTO;
import com.school.management.persistance.GroupEntity;

import java.util.List;
import java.util.Map;

public interface GroupService {
    List<GroupDTO> searchGroupsByNameStartingWithDTO(String name);
    void desactivateGroup(Long id);

    // updateGroupPartially(Long id, Map<String, Object> updates);
}
