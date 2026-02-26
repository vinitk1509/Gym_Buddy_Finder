package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.ActiveMatchDTO;
import com.vinit.gymPartner.dto.DashboardResponseDTO;
import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.dto.PendingMatchDTO;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.repository.*;
import lombok.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

        boolean profileComplete =
                fitnessProfileRepository.existsByUser_Id(userId);

        boolean hasActiveMatch =
                matchRepository.existsByRequesterOrReceiverAndStatus(
                        user, user, MatchStatus.ACCEPTED
                );

        int blockedCount =
                blockRepository.countByBlocker(user);

        List<MatchResultDTO> suggestions = new ArrayList<>();
        String message = null;

        List<AvailabilitySlot> slots =
                availabilitySlotRepository.findByUserId(userId);

        if (!user.isLookingForPartner()) {
            message = "Enable matching to see suggestions.";
        }
        else if (!profileComplete) {
            message = "Complete your profile to start matching.";
        } else if (slots.isEmpty()) {
            message = "Add availability to start matching.";
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

        List<ActiveMatchDTO> activeMatches =
                buildActiveMatches(user);

        List<PendingMatchDTO> pendingSent =
                buildPendingMatches(user, true);

        List<PendingMatchDTO> pendingReceived =
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
    private List<ActiveMatchDTO> buildActiveMatches(User user) {

        List<Match> matches =
                matchRepository.findAllByRequesterOrReceiverAndStatus(
                        user, user, MatchStatus.ACCEPTED
                );

        return matches.stream().map(match -> {

            User partner =
                    match.getRequester().equals(user)
                            ? match.getReceiver()
                            : match.getRequester();

            FitnessProfile profile =
                    partner.getFitnessProfile();

            return ActiveMatchDTO.builder()
                    .matchId(match.getId())
                    .partnerId(partner.getId())
                    .partnerName(partner.getName())
                    .partnerAge(partner.getAge())
                    .goal(profile.getGoal().name())
                    .experience(profile.getExperienceLevel().name())
                    .build();
        }).toList();
    }

    private List<PendingMatchDTO> buildPendingMatches(
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

        return matches.stream().map(match -> {

            User otherUser =
                    sent ? match.getReceiver() : match.getRequester();

            return PendingMatchDTO.builder()
                    .matchId(match.getId())
                    .userId(otherUser.getId())
                    .name(otherUser.getName())
                    .type(sent ? "SENT" : "RECEIVED")
                    .build();
        }).toList();
    }
}
