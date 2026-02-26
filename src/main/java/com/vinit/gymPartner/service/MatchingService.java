package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.ExploreFilterDTO;
import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingService {

    private final UserRepository userRepository;
    private final FitnessProfileRepository fitnessProfileRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final MatchRepository matchRepository;
    private final BlockRepository blockRepository;

    private static final double MATCH_THRESHOLD = 50.0;
    private static final int MAX_WEEKLY_TARGET_MINUTES = 300;

    public List<MatchResultDTO> findCompatibleUsers(Long userId) {

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateEligibility(currentUser);

        FitnessProfile currentProfile =
                fitnessProfileRepository.findByUser_Id(userId)
                        .orElseThrow(() -> new RuntimeException("Fitness profile missing"));

        List<AvailabilitySlot> currentSlots =
                availabilitySlotRepository.findByUserId(userId);

        if (currentSlots.isEmpty())
            throw new RuntimeException("Define availability before matching");

        // Step 1: Fetch all gym candidates
        List<User> candidates =
                userRepository.findByGymIdAndStatus(
                        currentUser.getGym().getId(),
                        UserStatus.ACTIVE
                );

        // Step 2: Bulk fetch profiles & slots
        List<Long> candidateIds =
                candidates.stream().map(User::getId).toList();

        Map<Long, FitnessProfile> profileMap =
                fitnessProfileRepository.findByUser_IdIn(candidateIds)
                        .stream()
                        .collect(Collectors.toMap(
                                fp -> fp.getUser().getId(),
                                fp -> fp
                        ));

        Map<Long, List<AvailabilitySlot>> slotMap =
                availabilitySlotRepository.findByUser_IdIn(candidateIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getUser().getId()
                        ));

        List<MatchResultDTO> results = new ArrayList<>();

        for (User candidate : candidates) {

            if (!isCandidateValid(currentUser, candidate))
                continue;

            FitnessProfile candidateProfile =
                    profileMap.get(candidate.getId());

            List<AvailabilitySlot> candidateSlots =
                    slotMap.get(candidate.getId());

            if (candidateProfile == null ||
                    candidateSlots == null ||
                    candidateSlots.isEmpty())
                continue;

            int overlapMinutes =
                    calculateWeeklyOverlap(currentSlots, candidateSlots);

            if (overlapMinutes <= 0)
                continue;

            double score =
                    calculateCompatibility(
                            currentUser,
                            candidate,
                            currentProfile,
                            candidateProfile,
                            overlapMinutes
                    );

            if (score >= MATCH_THRESHOLD) {

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

        results.sort(
                Comparator.comparingDouble(MatchResultDTO::getCompatibilityScore)
                        .reversed()
        );

        return results;
    }
    public List<MatchResultDTO> findExploreUsers(Long userId, ExploreFilterDTO filter) {

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateEligibility(currentUser);

        FitnessProfile currentProfile =
                fitnessProfileRepository.findByUser_Id(userId)
                        .orElseThrow(() -> new RuntimeException("Fitness profile missing"));

        List<AvailabilitySlot> currentSlots =
                availabilitySlotRepository.findByUserId(userId);

        if (currentSlots.isEmpty())
            throw new RuntimeException("Define availability before exploring");



        List<User> candidates =
                userRepository.findByGymIdAndStatus(
                        currentUser.getGym().getId(),
                        UserStatus.ACTIVE
                );

        List<MatchResultDTO> results = new ArrayList<>();

        for (User candidate : candidates) {


            if (!isCandidateValid(currentUser, candidate))
                continue;

            FitnessProfile candidateProfile =
                    fitnessProfileRepository.findByUser_Id(candidate.getId())
                            .orElse(null);

            List<AvailabilitySlot> candidateSlots =
                    availabilitySlotRepository.findByUserId(candidate.getId());

            if (candidateProfile == null || candidateSlots.isEmpty())
                continue;

            int overlapMinutes =
                    calculateWeeklyOverlap(currentSlots, candidateSlots);

            if (filter.getMinWeeklyOverlapMinutes() != null &&
                    overlapMinutes < filter.getMinWeeklyOverlapMinutes())
                continue;
            double score =
                    calculateCompatibility(
                            currentUser,
                            candidate,
                            currentProfile,
                            candidateProfile,
                            overlapMinutes
                    );

            // Apply dynamic filters
            int candidateAge = candidate.getAge();

            if (filter.getMinAge() != null &&
                    candidateAge < filter.getMinAge())
                continue;

            if (filter.getMaxAge() != null &&
                    candidateAge > filter.getMaxAge())
                continue;

            if (filter.getGoal() != null &&
                    candidateProfile.getGoal() != filter.getGoal())
                continue;

            if (filter.getExperience() != null &&
                    candidateProfile.getExperienceLevel() != filter.getExperience())
                continue;

            if (filter.getWorkoutType() != null &&
                    candidateProfile.getWorkoutType() != filter.getWorkoutType())
                continue;

            if (filter.getGender() != null &&
                    candidate.getGender() != filter.getGender())
                continue;

            if (filter.getMinScore() != null &&
                    score < filter.getMinScore())
                continue;

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

        results.sort(
                Comparator.comparingDouble(MatchResultDTO::getCompatibilityScore)
                        .reversed()
        );

        // Limit explore results to prevent overload
        return results.stream()
                .limit(20)
                .toList();
    }

    private void validateEligibility(User user) {

        if (user.getStatus() != UserStatus.ACTIVE)
            throw new RuntimeException("User not active");

        if (!user.isLookingForPartner())
            throw new RuntimeException("User disabled matching");
    }

    private boolean isCandidateValid(User current, User candidate) {

        FitnessProfile p1 = current.getFitnessProfile();
        FitnessProfile p2 = candidate.getFitnessProfile();

        int candidateAge = candidate.getAge();
        int currentAge = current.getAge();

        if (p1.getPreferredMinAge() != null &&
                candidateAge < p1.getPreferredMinAge())
            return false;

        if (p1.getPreferredMaxAge() != null &&
                candidateAge > p1.getPreferredMaxAge())
            return false;

        if (p2.getPreferredMinAge() != null &&
                currentAge < p2.getPreferredMinAge())
            return false;

        if (p2.getPreferredMaxAge() != null &&
                currentAge > p2.getPreferredMaxAge())
            return false;

        if (candidate.getId().equals(current.getId()))
            return false;

        if (!candidate.isLookingForPartner())
            return false;

        if (!current.getAllowMultiplePartners()
                && hasActiveMatch(current))
            return false;

        if (!candidate.getAllowMultiplePartners()
                && hasActiveMatch(candidate))
            return false;

        // Block check both directions
        if (blockRepository.existsByBlockerAndBlocked(current, candidate)
                || blockRepository.existsByBlockerAndBlocked(candidate, current))
            return false;

        // Active match
        if (matchExists(current, candidate, MatchStatus.ACCEPTED))
            return false;

        // Pending match
        if (matchExists(current, candidate, MatchStatus.PENDING))
            return false;

        return true;
    }

    private boolean matchExists(User u1, User u2, MatchStatus status) {
        return matchRepository.existsByRequesterAndReceiverAndStatus(u1, u2, status)
                || matchRepository.existsByRequesterAndReceiverAndStatus(u2, u1, status);
    }

    private boolean hasActiveMatch(User user) {
        return matchRepository.existsByRequesterOrReceiverAndStatus(
                user, user, MatchStatus.ACCEPTED
        );
    }

    private double calculateCompatibility(
            User u1,
            User u2,
            FitnessProfile p1,
            FitnessProfile p2,
            int overlapMinutes) {

        double timeScore = Math.min(
                (overlapMinutes / (double) MAX_WEEKLY_TARGET_MINUTES) * 30.0,
                30.0
        );

        double experienceScore =
                ExperienceScoringEngine.score(
                        p1.getExperienceLevel(),
                        p2.getExperienceLevel()
                );

        double ageScore = calculateAgeScore(u1.getAge(), u2.getAge());

        double goalScore =
                p1.getGoal() == p2.getGoal() ? 20.0 : 8.0;

        double workoutScore =
                p1.getWorkoutType() == p2.getWorkoutType() ? 10.0 : 4.0;

        double reliabilityScore =
                ((u1.getReliabilityScore()
                        + u2.getReliabilityScore()) / 2.0 / 100.0) * 10.0;

        double genderScore = 0.0;

        if (p1.getPreferredPartnerGender() == null ||
                p1.getPreferredPartnerGender() == u2.getGender())
            genderScore += 2.5;

        if (p2.getPreferredPartnerGender() == null ||
                p2.getPreferredPartnerGender() == u1.getGender())
            genderScore += 2.5;

        return timeScore
                + experienceScore
                + goalScore
                + workoutScore
                + reliabilityScore
                + genderScore
                + ageScore;
    }
    private double calculateAgeScore(int age1, int age2) {

        int diff = Math.abs(age1 - age2);

        if (diff <= 5) return 10.0;
        if (diff <= 10) return 7.0;
        if (diff <= 15) return 4.0;
        return 1.0;
    }

    private int calculateWeeklyOverlap(
            List<AvailabilitySlot> s1,
            List<AvailabilitySlot> s2) {


        int total = 0;

        for (AvailabilitySlot a : s1) {
            for (AvailabilitySlot b : s2) {

                if (a.getDayOfWeek() != b.getDayOfWeek())
                    continue;

                LocalTime start =
                        a.getStartTime().isAfter(b.getStartTime())
                                ? a.getStartTime()
                                : b.getStartTime();

                LocalTime end =
                        a.getEndTime().isBefore(b.getEndTime())
                                ? a.getEndTime()
                                : b.getEndTime();

                if (start.isBefore(end)) {
                    total += Duration.between(start, end).toMinutes();
                }
            }
        }

        return total;
    }
}