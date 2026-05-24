package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.DashboardResponseDTO;
import com.vinit.gymPartner.dto.MatchResponseDTO;
import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.repository.*;
import lombok.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final BlockRepository blockRepository;
    private final MatchingService matchingService;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final FitnessProfileRepository fitnessProfileRepository;

    public DashboardResponseDTO buildDashboard(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AvailabilitySlot> slots =
                availabilitySlotRepository.findByUserId(userId);

        boolean hasFitnessProfile =
                fitnessProfileRepository.existsByUser_Id(userId);

        boolean profileComplete =
                hasFitnessProfile && user.getGym() != null && !slots.isEmpty();

        boolean hasActiveMatch =
                user.getGym() != null
                        && matchRepository.findAllByUserAndStatus(user, MatchStatus.ACCEPTED)
                                .stream()
                                .anyMatch(match -> isVisibleToUser(match, user)
                                        && match.getGym() != null
                                        && match.getGym().getId().equals(user.getGym().getId()));

        int blockedCount =
                blockRepository.countByBlocker(user);

        List<MatchResultDTO> suggestions = new ArrayList<>();
        String message = null;

        if (!user.isLookingForPartner()) {
            message = "Enable matching to see suggestions.";
        }
        else if (!hasFitnessProfile || user.getGym() == null) {
            message = "Complete your profile to start matching.";
        } else if (slots.isEmpty()) {
            message = "Complete your profile by adding availability.";
        } else if (hasActiveMatch && !user.getAllowMultiplePartners()) {
            message = "You already have an active partner.";
        }
        else {
            suggestions = matchingService
                    .findCompatibleUsers(userId)
                    .stream()
                    .limit(10)
                    .toList();
        }

        List<MatchResponseDTO> activeMatches =
                buildActiveMatches(user);

        List<MatchResponseDTO> pendingSent =
                buildPendingMatches(user, true);

        List<MatchResponseDTO> pendingReceived =
                buildPendingMatches(user, false);

        return DashboardResponseDTO.builder()
                .matchingEnabled(user.isLookingForPartner())
                .profileComplete(profileComplete)
                .hasActiveMatch(hasActiveMatch)
                .blockedCount(blockedCount)
                .suggestedPartners(suggestions)
                .activeMatches(activeMatches)
                .pendingSentRequests(pendingSent)
                .pendingReceivedRequests(pendingReceived)
                .message(message)
                .build();
    }

    private List<MatchResponseDTO> buildActiveMatches(User user) {

        List<Match> matches =
                matchRepository.findAllByUserAndStatus(user, MatchStatus.ACCEPTED);

        return matches.stream()
                .filter(match -> isVisibleToUser(match, user))
                .map(this::mapToDTO)
                .toList();
    }

    private List<MatchResponseDTO> buildPendingMatches(
            User user,
            boolean sent
    ) {

        List<Match> matches;

        if (sent) {
            matches = matchRepository
                    .findByRequesterAndStatus(user, MatchStatus.PENDING);
        } else {
            matches = matchRepository
                    .findByReceiverIdAndStatus(user.getId(), MatchStatus.PENDING);
        }

        return matches.stream()
                .filter(match -> isVisibleToUser(match, user))
                .map(this::mapToDTO)
                .toList();
    }

    private boolean isVisibleToUser(Match match, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        return match.getRequester().getRole() != UserRole.ADMIN
                && match.getReceiver().getRole() != UserRole.ADMIN;
    }

    private MatchResponseDTO mapToDTO(Match match) {

        MatchResponseDTO dto = new MatchResponseDTO();

        dto.setId(match.getId());

        User requester = match.getRequester();
        dto.setRequesterId(requester.getId());
        dto.setRequesterEmail(requester.getEmail());
        dto.setRequesterName(requester.getName());
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
        dto.setReceiverName(receiver.getName());
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
            dto.setTerminatedByName(match.getTerminatedBy().getName());
        }

        return dto;
    }

    private boolean isActiveNow(User user) {
        return user.getLastSeenAt() != null
                && user.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(5));
    }
}
