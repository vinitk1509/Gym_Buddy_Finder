package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final UserRepository userRepository;

    @PostMapping("/request")
    public ResponseEntity<Match> sendRequest(
            @RequestParam Long receiverId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User requester = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found"));

        Match match = matchService.sendMatchRequest(requester.getId(), receiverId);
        return ResponseEntity.ok(match);
    }

    @PostMapping("/{matchId}/accept")
    public ResponseEntity<Match> acceptMatch(
            @PathVariable Long matchId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        Match updatedMatch = matchService.acceptmatch(matchId, email);

        return ResponseEntity.ok(updatedMatch);
    }
}