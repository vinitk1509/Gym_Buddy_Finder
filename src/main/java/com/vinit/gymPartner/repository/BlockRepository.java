package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Block;
import com.vinit.gymPartner.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    List<Block> findByBlocker(User blocker);

    @Query("""
        SELECT COUNT(b) > 0 FROM Block b
        WHERE (b.blocker = :user1 AND b.blocked = :user2)
           OR (b.blocker = :user2 AND b.blocked = :user1)
    """)
    boolean existsBlockBetweenUsers(@Param("user1") User user1,
                                    @Param("user2") User user2);

    @Transactional
    void deleteByBlockerAndBlocked(User blocker, User blocked);


    boolean existsByBlockedAndBlocker(User blocked, User blocker);

    int countByBlocker(User blocker);
}