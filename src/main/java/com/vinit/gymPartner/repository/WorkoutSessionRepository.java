package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
}
