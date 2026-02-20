package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByGymIdAndStatusAndLookingForPartnerTrue(
            Long gymId,
            UserStatus status,
            Pageable pageable
    );
    List<User> findByGymIdAndStatus(Long gymId, UserStatus status);
    boolean existsByEmail(String email);
}
