package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.ExperienceLevel;
import com.vinit.gymPartner.entity.enums.FitnessGoal;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.AvailabilitySlotRepository;
import com.vinit.gymPartner.repository.FitnessProfileRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly= true)
public class MatchingService {

    private final UserRepository userRepository;
    private final FitnessProfileRepository fitnessProfileRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final MatchRepository matchRepository;

    private static final double MATCH_THRESHOLD = 40.0;
    private static final int MAX_WEEKLY_TARGET_MINUTES = 300;

    public Page<MatchResultDTO> findCompatibleUsers(Long userId, Pageable pageable)
    {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("USER NOT FOUND!"));

        validateUserEligibility(currentUser);

        FitnessProfile currentProfile = fitnessProfileRepository.findByUserId(userId)
                .orElseThrow(()->new RuntimeException("FITNESS PROFILE MISSING"));

        List<AvailabilitySlot> currentSlots = availabilitySlotRepository.findByUserId(userId);

        if(currentSlots.isEmpty()){
            throw new RuntimeException("NO AVAILABILITY SLOTS DEFINED");
        }

        Set<Long> excludedUserIds = getExcludedUserIds(userId);

        Page<User> candidatePage =
                userRepository.findByGymIdAndStatusAndLookingForPartnerTrue(
                        currentUser.getGym().getId(),
                        UserStatus.ACTIVE,
                        pageable
                );

        List<User> filteredCandidates = candidatePage.getContent().stream()
                .filter(u -> !u.getId().equals(userId))
                .filter(u -> !excludedUserIds.contains(u.getId()))
                .toList();

        if (filteredCandidates.isEmpty())
            return Page.empty();

        List<Long> candidateIds =
                filteredCandidates.stream().map(User::getId).toList();

        //========BULK FETCH====

//        List<FitnessProfile> profiles = fitnessProfileRepository.findByUserIsIn(candidatesIds);
//        List<AvailabilitySlot> allSlots = availabilitySlotRepository.findByUserIdIn(candidatesIds);

        //======map profiles by userId
        Map<Long, FitnessProfile> profileMap =
                fitnessProfileRepository.findByUserIsIn(candidateIds).stream()
                        .collect(Collectors.toMap(
                                fp->fp.getUser().getId(),
                                fp->fp
                        ));

        //=======Group Slots by userId

        Map<Long, List<AvailabilitySlot>> slotMap =
                availabilitySlotRepository.findByUserIdIn(candidateIds).stream()
                        .collect(Collectors.groupingBy(
                                slot->slot.getUser().getId()
                        ));

        List<MatchResultDTO> results = new ArrayList<>();

        for (User candidate : filteredCandidates)
        {
            FitnessProfile candidateProfile = profileMap.get(candidate.getId());
            List<AvailabilitySlot> candidateSlots = slotMap.get(candidate.getId());

            if(candidateProfile == null || candidateSlots == null || candidateSlots.isEmpty()) continue;

            int overlapMinutes = calculateWeeklyOverlap(currentSlots, candidateSlots);

            if (overlapMinutes == 0) continue;

            double score = calculateCompatibility(
                    currentUser,
                    candidate,
                    currentProfile,
                    candidateProfile,
                    overlapMinutes
            );
            if (score >= MATCH_THRESHOLD){
                results.add(
                        MatchResultDTO.builder()
                                .userId(candidate.getId())
                                .fullName(candidate.getName())
                                .experienceLevel(candidateProfile.getExperienceLevel().name())
                                .goal(candidateProfile.getGoal().name())
                                .workoutType(candidateProfile.getWorkoutType().name())
                                .compatibilityScore(score)
                                .build()
                );
            }
        }
        results.sort(Comparator.comparingDouble(MatchResultDTO::getCompatibilityScore)
                .reversed()
                .thenComparing(MatchResultDTO::getUserId));
        return new PageImpl<>(results, pageable, candidatePage.getTotalElements());
    }

    private void validateUserEligibility(User user) {

        if (user.getStatus() != UserStatus.ACTIVE)
            throw new RuntimeException("User is not active");

        if (!user.isLookingForPartner())
            throw new RuntimeException("User disabled partner matching");
    }

    private Set<Long> getExcludedUserIds(Long userId) {

        List<Match> matches =
                matchRepository.findByUser1IdOrUser2Id(userId, userId);

        Set<Long> excluded = new HashSet<>();

        for (Match match : matches) {
            if (match.getUser1().getId().equals(userId))
                excluded.add(match.getUser2().getId());
            else
                excluded.add(match.getUser1().getId());
        }

        return excluded;
    }


    private double calculateCompatibility(User u1, User u2, FitnessProfile p1, FitnessProfile p2, int overlapMinutes) {

        double timeScore = Math.min(
                (overlapMinutes/ 300.0)*30.0, 30.0);

        double experienceScore = ExperienceScoringEngine.score(p1.getExperienceLevel(), p2.getExperienceLevel());

        double goalScore = p1.getGoal() == p2.getGoal() ? 25.0 : 10.0;

        double reliabilityScore = ((u1.getReliabilityScore()+u2.getReliabilityScore()) / 2.0 /100.0)*10.0;

        double workoutScore = p1.getWorkoutType() == p2.getWorkoutType() ? 10.0 : 5.0;

        return timeScore + experienceScore + goalScore + reliabilityScore + workoutScore;
    }

    private int calculateWeeklyOverlap(List<AvailabilitySlot> slots1, List<AvailabilitySlot> slots2) {
        int total = 0;

        for (AvailabilitySlot s1 : slots1){
            for (AvailabilitySlot s2 : slots2){
                if (s1.getDayOfWeek() != s2.getDayOfWeek()) continue;

                LocalTime start = s1.getStartTime().isAfter(s2.getStartTime())
                        ? s1.getStartTime()
                        : s2.getStartTime();

                LocalTime end = s1.getEndTime().isBefore(s2.getEndTime())
                        ? s1.getEndTime()
                        : s2.getEndTime();

                if (start.isBefore(end)) {
                    total += Duration.between(start, end).toMinutes();
                }
            }
        }
        return total;
    }


    private static class UserScoreWrapper {
        User user;
        double score;

        UserScoreWrapper(User user, double score) {
            this.user = user;
            this.score = score;
        }
    }
}
