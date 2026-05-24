package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.GymGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymGroupRepository extends JpaRepository<GymGroup, Long> {
    List<GymGroup> findByGymId(Long gymId);
    List<GymGroup> findByMembersId(Long userId);
}
