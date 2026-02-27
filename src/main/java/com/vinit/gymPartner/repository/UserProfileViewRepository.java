package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.UserProfileView;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserProfileViewRepository
        extends JpaRepository<UserProfileView, Long> {

    Optional<UserProfileView>
    findByViewerAndViewedUser(User viewer, User viewedUser);

    @Query("""
        SELECT v.viewedUser.id
        FROM UserProfileView v
        WHERE v.viewer.id = :viewerId
    """)
    List<Long> findViewedUserIds(@Param("viewerId") Long viewerId);
}