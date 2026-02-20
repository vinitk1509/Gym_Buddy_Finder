package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
}
