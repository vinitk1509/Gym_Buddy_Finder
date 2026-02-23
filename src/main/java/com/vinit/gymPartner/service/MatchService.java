package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public Match sendMatchRequest(Long requesterId, Long receiverId)
    {
        if (requesterId.equals(receiverId)){
            throw new IllegalArgumentException("You cannot match with yourself");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(()->new RuntimeException("Requester not found"));
        User receiver = userRepository.findById(requesterId)
                .orElseThrow(()->new RuntimeException("receiver not found"));

        Match existingMatch = matchRepository
                .findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(
                        requesterId,receiverId,
                        receiverId,requesterId
                ).orElse(null);

        if (existingMatch != null){
            switch (existingMatch.getStatus()){
                case PENDING:
                    if (existingMatch.getRequester().getId().equals(receiverId)){
                        existingMatch.setStatus(MatchStatus.ACCEPTED);
                        return matchRepository.save(existingMatch);
                    }
                    throw new IllegalStateException("Match request already pending");
                case ACCEPTED:
                    throw new IllegalStateException("Users are already matched");
                case TERMINATED:
                case REJECTED:
                case CANCELLED:
                    break;

                default: throw new IllegalStateException("Unexpected match state");
            }
        }
        double compatibilityScore = calculateCompatibility(requester, receiver);

        Match newMatch = Match.builder()
                .requester(requester)
                .receiver(receiver)
                .status(MatchStatus.PENDING)
                .compatibilityScore(compatibilityScore)
                .build();

        return matchRepository.save(newMatch);
    }

    public Match acceptmatch(Long matchId, String userEmail)
    {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(()->new RuntimeException("Match not found"));

        if (match.getStatus() != MatchStatus.PENDING){
            throw new IllegalStateException("Only pending matches can be accepted");
        }

        User loggedInUser = userRepository.findByEmail(userEmail)
                .orElseThrow(()->new RuntimeException("User not found"));

        // ---------only receivers can accept------------
        if (!match.getReceiver().getId().equals(loggedInUser.getId())){
            throw new IllegalStateException("Only receivers can accept this match");
        }

        match.setStatus(MatchStatus.ACCEPTED);
        return matchRepository.save(match);
    }

    private double calculateCompatibility(User requester, User receiver) {
        // TEMPORARY: we will replace this later with real algorithm
        return 75.0;
    }
}