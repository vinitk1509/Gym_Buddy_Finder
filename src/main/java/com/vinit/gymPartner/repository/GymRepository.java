package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GymRepository extends JpaRepository<Gym, Long> {
    Optional<Gym> findByNameAndAddress(String name, String address);
}
