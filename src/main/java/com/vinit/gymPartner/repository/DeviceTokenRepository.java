package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUserId(Long userId);
    Optional<DeviceToken> findByToken(String token);

    @Transactional
    void deleteByUserId(Long userId);
}
