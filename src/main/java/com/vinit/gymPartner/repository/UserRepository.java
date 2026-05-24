package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByGymIdAndStatusAndLookingForPartnerTrue(
            Long gymId,
            UserStatus status,
            Pageable pageable
    );

    List<User> findByGymIdAndStatus(Long gymId, UserStatus status);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByGymNameAndGymAddressAndLookingForPartnerTrue(
            String gymName,
            String gymAddress
    );

    Optional<User> findByEmailAndStatus(String email, UserStatus status);
    Optional<User> findByEmailIgnoreCaseAndStatus(String email, UserStatus status);

    @Query("""
            SELECT u FROM User u
            WHERE u.status = 'ACTIVE'
            AND u.role <> 'ADMIN'
            AND u.lookingForPartner = true
            AND u.id != :currentUserId
            AND u.id NOT IN (
                SELECT v.viewedUser.id
                FROM UserProfileView v
                WHERE v.viewer.id = :currentUserId
                AND v.viewedAt >= :sevenDaysAgo
            )
            """)
    List<User> findSuggestedUsers(Long currentUserId, LocalDateTime sevenDaysAgo);
    List<User> findByStatus(UserStatus status);
    long countByStatus(UserStatus status);
    long countByRole(UserRole role);

    List<User> findByStatusAndDeletionRequestedAtBefore(UserStatus status, LocalDateTime cutoff);

    List<User> findByStatusNotAndLastLoginAtBefore(UserStatus status, LocalDateTime cutoff);
}
