package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.MatchResponseDTO;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.repository.BlockRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.LifecycleState;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    public MatchResponseDTO sendMatchRequest(Long requesterId, Long receiverId)
    {
        if (requesterId.equals(receiverId)){
            throw new IllegalArgumentException("You cannot match with yourself");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(()->new RuntimeException("Requester not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(()->new RuntimeException("receiver not found"));

        if (blockRepository.existsBlockBetweenUsers(requester, receiver)) {
            throw new RuntimeException("You cannot interact with this user");
        }

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
                        return mapToDTO(matchRepository.save(existingMatch));
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

        Match savedMatch = matchRepository.save(newMatch);

        return mapToDTO(savedMatch);
    }

    public MatchResponseDTO acceptmatch(Long matchId, String userEmail)
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
        return mapToDTO(matchRepository.save(match));
    }

    public MatchResponseDTO rejectMatch(Long matchId, Authentication authentication)
    {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found"));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(()->new RuntimeException("Match not found"));

        if (!match.getReceiver().getId().equals(currentUser.getId())){
            throw new RuntimeException("Only receiver can reject this request");
        }
        if (match.getStatus() != MatchStatus.PENDING){
            throw new RuntimeException("Only pending matches can be rejected");
        }
        match.setStatus(MatchStatus.REJECTED);
        match.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(matchRepository.save(match));
    }

    public MatchResponseDTO cancelMatch(Long matchId, Authentication authentication)
    {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found"));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(()->new RuntimeException("Match not found"));

        if (!match.getRequester().getId().equals(currentUser.getId())){
            throw new RuntimeException("Only requester can reject this match request");
        }
        if (match.getStatus() != MatchStatus.PENDING){
            throw new RuntimeException("Only pending matches can be rejected");
        }

        match.setStatus(MatchStatus.CANCELLED);
        match.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(matchRepository.save(match));
    }


    public MatchResponseDTO terminateMatch(Long matchId, Authentication authentication)
    {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found"));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(()->new RuntimeException("Match not found"));

        boolean isParticipant =
                match.getReceiver().getId().equals(currentUser.getId());
                match.getRequester().getId().equals(currentUser.getId());

                if (!isParticipant){
                    throw new RuntimeException("You are not part of this match");
                }

                if (match.getStatus() != MatchStatus.ACCEPTED){
                    throw new RuntimeException("Only accepted matches can be terminated");
                }

                match.setStatus(MatchStatus.TERMINATED);
                match.setTerminatedAt(LocalDateTime.now());
                match.setTerminatedBy(currentUser);
                match.setUpdatedAt(LocalDateTime.now());

                return mapToDTO(matchRepository.save(match));
    }

    public List<MatchResponseDTO> getMySentRequests(Authentication authentication){
        User user = getCurrentUser(authentication);

        return matchRepository
                .findByRequesterIdAndStatus(user.getId(), MatchStatus.PENDING)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<MatchResponseDTO> getMyReceivedRequests(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return matchRepository
                .findByReceiverIdAndStatus(user.getId(), MatchStatus.PENDING)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<MatchResponseDTO> getMyActiveMatches(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return matchRepository
                .findByStatusAndRequesterIdOrStatusAndReceiverId(
                        MatchStatus.ACCEPTED, user.getId(),
                        MatchStatus.ACCEPTED, user.getId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<MatchResponseDTO> getMyMatchHistory(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return matchRepository
                .findByRequesterIdOrReceiverId(user.getId(), user.getId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    private double calculateCompatibility(User requester, User receiver) {
        // TEMPORARY: we will replace this later with real algorithm
        return 75.0;
    }

    private MatchResponseDTO mapToDTO(Match match) {

        MatchResponseDTO dto = new MatchResponseDTO();

        dto.setId(match.getId());
        dto.setRequesterId(match.getRequester().getId());
        dto.setRequesterEmail(match.getRequester().getEmail());

        dto.setReceiverId(match.getReceiver().getId());
        dto.setReceiverEmail(match.getReceiver().getEmail());

        dto.setStatus(match.getStatus().name());
        dto.setCompatibilityScore(match.getCompatibilityScore());

        dto.setCreatedAt(match.getCreatedAt());
        dto.setUpdatedAt(match.getUpdatedAt());
        dto.setTerminatedAt(match.getTerminatedAt());

        return dto;
    }
}