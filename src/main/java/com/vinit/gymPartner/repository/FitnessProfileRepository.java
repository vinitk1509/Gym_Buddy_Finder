package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.FitnessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FitnessProfileRepository extends JpaRepository<FitnessProfile, Long> {

    Optional<FitnessProfile> findByUser_Id(Long userId);
    List<FitnessProfile> findByUser_IdIn(List<Long> userIds);
    boolean existsByUser_Id(Long userId);
}
