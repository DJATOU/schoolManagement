package com.school.management.repository;

import com.school.management.persistance.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    List<RoomEntity> findByCapacityGreaterThanEqual(Integer capacity);
    List<RoomEntity> findByNameContaining(String name);

    // Add other custom methods if needed
}
