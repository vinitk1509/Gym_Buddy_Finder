package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByUserId(Long userId);
    List<AvailabilitySlot> findByUserIdIn(List<Long> userIds);
}
