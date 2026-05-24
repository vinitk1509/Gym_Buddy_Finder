package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.MatchResponseDTO;
import com.vinit.gymPartner.dto.UserResponseDTO;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.BlockRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.repository.AvailabilitySlotRepository;
import com.vinit.gymPartner.repository.WorkoutSessionRepository;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.FitnessProfile;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmailService emailService;
    private final UserService userService;
    private final UserProfileViewService userProfileViewService;
    private final WorkoutSessionRepository workoutSessionRepository;


    private static final int DAILY_LIMIT = 10;

    @Transactional
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

        if (requester.getRole() == UserRole.ADMIN || receiver.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Admin accounts cannot use regular matching");
        }

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
        // ----------------------------------------

        List<Match> existingMatches =
                matchRepository.findMatchesBetweenUsers(requesterId, receiverId);

        Match existingMatch = existingMatches.isEmpty() ? null : existingMatches.get(0);

        Match matchToSave;
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
                    // Reuse the existing match entity instead of duplicating!
                    workoutSessionRepository.deleteByMatch(existingMatch);
                    existingMatch.setStatus(MatchStatus.PENDING);
                    existingMatch.setGym(requester.getGym());
                    existingMatch.setRequester(requester);
                    existingMatch.setReceiver(receiver);
                    existingMatch.setCompatibilityScore(calculateCompatibility(requester, receiver));
                    existingMatch.setExpiresAt(LocalDateTime.now().plusDays(7));
                    matchToSave = existingMatch;
                    break;
                default: 
                    throw new IllegalStateException("Unexpected match state");
            }
        } else {
            double compatibilityScore = calculateCompatibility(requester, receiver);
            matchToSave = Match.builder()
                    .gym(requester.getGym())
                    .requester(requester)
                    .receiver(receiver)
                    .status(MatchStatus.PENDING)
                    .compatibilityScore(compatibilityScore)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
        }

        Match savedMatch = matchRepository.save(matchToSave);
        emailService.sendMatchRequestEmail(receiver.getEmail(), receiver.getName(), requester.getName());
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
        Match savedMatch = matchRepository.save(match);

        return mapToDTO(savedMatch);
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

        Match savedMatch = matchRepository.save(match);

        return mapToDTO(savedMatch);
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

        Match savedMatch = matchRepository.save(match);

        return mapToDTO(savedMatch);
    }


    @Transactional
    public MatchResponseDTO terminateMatch(Long matchId, Authentication authentication)
    {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found"));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(()->new RuntimeException("Match not found"));

        boolean isParticipant =
                match.getReceiver().getId().equals(currentUser.getId()) ||
                match.getRequester().getId().equals(currentUser.getId());

        if (!isParticipant) {
            throw new RuntimeException("You are not part of this match");
        }

        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new RuntimeException("Only accepted matches can be terminated");
        }

        match.setStatus(MatchStatus.TERMINATED);
        match.setTerminatedAt(LocalDateTime.now());
        match.setTerminatedBy(currentUser);
        match.setUpdatedAt(LocalDateTime.now());

        workoutSessionRepository.deleteByMatch(match);

        userService.updateReliability(currentUser, -5);

        Match savedMatch = matchRepository.save(match);

        return mapToDTO(savedMatch);
    }

    public List<MatchResponseDTO> getMySentRequests(Authentication authentication){
        User user = getCurrentUser(authentication);

        return matchRepository
                .findByRequesterIdAndStatus(user.getId(), MatchStatus.PENDING)
                .stream()
                .filter(match -> isVisibleToUser(match, user))
                .map(this::mapToDTO)
                .toList();
    }

    public List<MatchResponseDTO> getMyReceivedRequests(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return matchRepository
                .findByReceiverIdAndStatus(user.getId(), MatchStatus.PENDING)
                .stream()
                .filter(match -> isVisibleToUser(match, user))
                .map(this::mapToDTO)
                .toList();
    }

    public List<MatchResponseDTO> getMyActiveMatches(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return matchRepository
                .findByRequesterIdOrReceiverId(user.getId(), user.getId())
                .stream()
                .filter(m -> m.getStatus() == MatchStatus.ACCEPTED)
                .filter(match -> isVisibleToUser(match, user))
                .map(this::mapToDTO)
                .toList();
    }

    public List<MatchResponseDTO> getMyMatchHistory(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return matchRepository
                .findByRequesterIdOrReceiverId(user.getId(), user.getId())
                .stream()
                .filter(match -> isVisibleToUser(match, user))
                .map(this::mapToDTO)
                .toList();
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isVisibleToUser(Match match, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        return match.getRequester().getRole() != UserRole.ADMIN
                && match.getReceiver().getRole() != UserRole.ADMIN;
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

        User requester = match.getRequester();
        dto.setRequesterId(requester.getId());
        dto.setRequesterEmail(requester.getEmail());
        dto.setRequesterName(ChatService.displayName(requester));
        dto.setRequesterProfilePicture(requester.getProfilePictureUrl());
        dto.setRequesterGymName(requester.getGym() != null ? requester.getGym().getName() : null);
        dto.setRequesterAge(requester.getAge());
        dto.setRequesterReliabilityScore(requester.getReliabilityScore());
        dto.setRequesterActiveNow(isActiveNow(requester));
        dto.setRequesterTargetGroupSize(requester.getTargetGroupSize());
        if (requester.getFitnessProfile() != null) {
            FitnessProfile rp = requester.getFitnessProfile();
            dto.setRequesterFitnessGoal(rp.getGoal() != null ? rp.getGoal().name() : null);
            dto.setRequesterWorkoutType(rp.getWorkoutType() != null ? rp.getWorkoutType().name() : null);
            dto.setRequesterExperienceLevel(rp.getExperienceLevel() != null ? rp.getExperienceLevel().name() : null);
        }

        User receiver = match.getReceiver();
        dto.setReceiverId(receiver.getId());
        dto.setReceiverEmail(receiver.getEmail());
        dto.setReceiverName(ChatService.displayName(receiver));
        dto.setReceiverProfilePicture(receiver.getProfilePictureUrl());
        dto.setReceiverGymName(receiver.getGym() != null ? receiver.getGym().getName() : null);
        dto.setReceiverAge(receiver.getAge());
        dto.setReceiverReliabilityScore(receiver.getReliabilityScore());
        dto.setReceiverActiveNow(isActiveNow(receiver));
        dto.setReceiverTargetGroupSize(receiver.getTargetGroupSize());
        if (receiver.getFitnessProfile() != null) {
            FitnessProfile rcp = receiver.getFitnessProfile();
            dto.setReceiverFitnessGoal(rcp.getGoal() != null ? rcp.getGoal().name() : null);
            dto.setReceiverWorkoutType(rcp.getWorkoutType() != null ? rcp.getWorkoutType().name() : null);
            dto.setReceiverExperienceLevel(rcp.getExperienceLevel() != null ? rcp.getExperienceLevel().name() : null);
        }

        dto.setStatus(match.getStatus().name());
        dto.setCompatibilityScore(match.getCompatibilityScore());

        dto.setCreatedAt(match.getCreatedAt());
        dto.setUpdatedAt(match.getUpdatedAt());
        dto.setTerminatedAt(match.getTerminatedAt());

        if (match.getTerminatedBy() != null) {
            dto.setTerminatedById(match.getTerminatedBy().getId());
            dto.setTerminatedByName(ChatService.displayName(match.getTerminatedBy()));
        }

        return dto;
    }

    private boolean isActiveNow(User user) {
        return user.getLastSeenAt() != null
                && user.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(5));
    }
}
