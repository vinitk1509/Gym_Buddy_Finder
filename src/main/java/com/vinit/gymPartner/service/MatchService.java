package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.MatchResponseDTO;
import com.vinit.gymPartner.dto.UserResponseDTO;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.BlockRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.repository.AvailabilitySlotRepository;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.FitnessProfile;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.LifecycleState;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final MatchingService matchingService;
    private UserService userService;
    private Match match;
    private final UserProfileViewService userProfileViewService;


    private static final int DAILY_LIMIT = 10;

    public MatchResponseDTO sendMatchRequest(Long requesterId, Long receiverId)
    {
        LocalDateTime startOfDay =
                LocalDate.now().atStartOfDay();


        if (requesterId.equals(receiverId)){
            throw new IllegalArgumentException("You cannot match with yourself");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(()->new RuntimeException("Requester not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(()->new RuntimeException("receiver not found"));

        long todayCount =
                matchRepository.countTodayRequests(
                        requester.getId(),
                        startOfDay
                );

        if (todayCount >= DAILY_LIMIT)
            throw new RuntimeException(
                    "Daily match request limit reached. Try again tomorrow."
            );

        if (blockRepository.existsBlockBetweenUsers(requester, receiver)) {
            throw new RuntimeException("You cannot interact with this user");
        }
        if (receiver.getStatus() != UserStatus.ACTIVE)
            throw new RuntimeException("User account inactive");

        // --- NEW REAL MATCHING RESTRICTIONS ---
        if (!requester.getGym().getId().equals(receiver.getGym().getId())) {
            throw new RuntimeException("Users must belong to the exact same Gym to match.");
        }

        FitnessProfile requesterProfile = requester.getFitnessProfile();
        FitnessProfile receiverProfile = receiver.getFitnessProfile();
        
        if (requesterProfile == null || receiverProfile == null) {
            throw new RuntimeException("Both users must have completed their fitness profiles.");
        }

        if (requesterProfile.getGoal() != receiverProfile.getGoal()) {
            throw new RuntimeException("Matching requires having the exact same fitness goal.");
        }
        // ----------------------------------------

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
        match.setCreatedAt(LocalDateTime.now());
        match.setExpiresAt(LocalDateTime.now().plusDays(7));
        match.setStatus(MatchStatus.PENDING);

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

        if (loggedInUser.getStatus() != UserStatus.ACTIVE)
            throw new RuntimeException("User account inactive");

        if (match.getStatus() == MatchStatus.EXPIRED)
            throw new RuntimeException("Match request has expired.");

        // ---------only receivers can accept------------
        if (!match.getReceiver().getId().equals(loggedInUser.getId())){
            throw new IllegalStateException("Only receivers can accept this match");
        }
        userService.updateReliability(match.getRequester(), +2);
        userService.updateReliability(match.getReceiver(), +2);

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
        if (match.getStatus() == MatchStatus.EXPIRED){
            throw new RuntimeException("Match request has expired.");
        }
        if (match.getStatus() != MatchStatus.PENDING){
            throw new RuntimeException("Only pending matches can be rejected");
        }
        match.setStatus(MatchStatus.REJECTED);
        match.setUpdatedAt(LocalDateTime.now());

        userService.updateReliability(match.getRequester(), -1);

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

        userService.updateReliability(match.getRequester(), -2);

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

        userService.updateReliability(currentUser, -5);

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

    public List<UserResponseDTO> getSuggestedUsers(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<User> users = userRepository
                .findSuggestedUsers(currentUser.getId(), sevenDaysAgo);

        return users.stream()
                .map(userService::convertToResponseDTO)
                .toList();
    }


    private double calculateCompatibility(User requester, User receiver) {
        
        FitnessProfile p1 = requester.getFitnessProfile();
        FitnessProfile p2 = receiver.getFitnessProfile();
        
        List<AvailabilitySlot> slots1 = availabilitySlotRepository.findByUserId(requester.getId());
        List<AvailabilitySlot> slots2 = availabilitySlotRepository.findByUserId(receiver.getId());
        
        int overlapMinutes = matchingService.calculateWeeklyOverlap(slots1, slots2);
        
        return matchingService.calculateCompatibility(requester, receiver, p1, p2, overlapMinutes);
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