package com.school.management.service.interfaces;

import com.school.management.dto.GroupDTO;

import java.util.List;

public interface GroupService {
    List<GroupDTO> searchGroupsByNameStartingWithDTO(String name);
    void desactivateGroup(Long id);
}
