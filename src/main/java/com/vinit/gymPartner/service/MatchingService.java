package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.ExploreFilterDTO;
import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserRole;
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
            return Collections.emptyList();

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
                    slotMap.getOrDefault(candidate.getId(), Collections.emptyList());

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
                                .age(candidate.getAge() != null ? candidate.getAge() : 0)
                                .experienceLevel(candidateProfile.getExperienceLevel().name())
                                .goal(candidateProfile.getGoal().name())
                                .workoutType(candidateProfile.getWorkoutType().name())
                                .compatibilityScore(score)
                                .profilePictureUrl(candidate.getProfilePictureUrl())
                                .gymName(candidate.getGym() != null ? candidate.getGym().getName() : null)
                                .bio(candidate.getBio())
                                .reliabilityScore(candidate.getReliabilityScore())
                                .relationshipStatus(null)
                                .canSendRequest(true)
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



        List<User> candidates =
                userRepository.findByGymIdAndStatus(
                        currentUser.getGym().getId(),
                        UserStatus.ACTIVE
                );

        // If radiusKm is set, expand candidates beyond just same gym
        if (filter.getRadiusKm() != null && filter.getRadiusKm() > 0) {
            List<User> allActive = userRepository.findByStatus(UserStatus.ACTIVE);
            candidates = allActive.stream()
                    .filter(c -> {
                        if (c.getGym() == null || c.getGym().getLatitude() == null || c.getGym().getLongitude() == null) return false;
                        if (currentUser.getGym().getLatitude() == null || currentUser.getGym().getLongitude() == null) return false;
                        double dist = haversine(
                                currentUser.getGym().getLatitude(), currentUser.getGym().getLongitude(),
                                c.getGym().getLatitude(), c.getGym().getLongitude());
                        return dist <= filter.getRadiusKm();
                    })
                    .toList();
        }

        List<MatchResultDTO> results = new ArrayList<>();

        for (User candidate : candidates) {


            if (!isExploreCandidateVisible(currentUser, candidate))
                continue;

            FitnessProfile candidateProfile =
                    fitnessProfileRepository.findByUser_Id(candidate.getId())
                            .orElse(null);

            List<AvailabilitySlot> candidateSlots =
                    availabilitySlotRepository.findByUserId(candidate.getId());

            if (candidateProfile == null)
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
            Integer candidateAge = candidate.getAge();

            if (filter.getMinAge() != null &&
                    (candidateAge == null || candidateAge < filter.getMinAge()))
                continue;

            if (filter.getMaxAge() != null &&
                    (candidateAge == null || candidateAge > filter.getMaxAge()))
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

            results.add(buildExploreResult(currentUser, candidate, candidateProfile, score));
        }

        results.sort(
                Comparator.comparingDouble(MatchResultDTO::getCompatibilityScore)
                        .reversed()
        );

        return results;
    }

    private void validateEligibility(User user) {

        if (user.getStatus() != UserStatus.ACTIVE)
            throw new RuntimeException("User not active");

        if (user.getRole() == UserRole.ADMIN)
            throw new RuntimeException("Admin accounts cannot use matching");

        if (!user.isLookingForPartner())
            throw new RuntimeException("User disabled matching");
    }

    private boolean isCandidateValid(User current, User candidate) {

        if (candidate.getId().equals(current.getId()))
            return false;

        if (candidate.getRole() == UserRole.ADMIN)
            return false;

        if (!candidate.isLookingForPartner())
            return false;

        FitnessProfile p1 = current.getFitnessProfile();
        FitnessProfile p2 = candidate.getFitnessProfile();

        // Skip age preference checks if either profile is null
        if (p1 != null && p2 != null) {
            Integer candidateAge = candidate.getAge();
            Integer currentAge = current.getAge();

            if (candidateAge != null && p1.getPreferredMinAge() != null &&
                    candidateAge < p1.getPreferredMinAge())
                return false;

            if (candidateAge != null && p1.getPreferredMaxAge() != null &&
                    candidateAge > p1.getPreferredMaxAge())
                return false;

            if (currentAge != null && p2.getPreferredMinAge() != null &&
                    currentAge < p2.getPreferredMinAge())
                return false;

            if (currentAge != null && p2.getPreferredMaxAge() != null &&
                    currentAge > p2.getPreferredMaxAge())
                return false;
        }

        if (!current.getAllowMultiplePartners()
                && hasActiveMatchAtCurrentGym(current))
            return false;

        if (!candidate.getAllowMultiplePartners()
                && hasActiveMatchAtCurrentGym(candidate))
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

    private boolean isExploreCandidateVisible(User current, User candidate) {

        if (candidate.getId().equals(current.getId()))
            return false;

        if (candidate.getRole() == UserRole.ADMIN)
            return false;

        if (!candidate.isLookingForPartner())
            return false;

        return !blockRepository.existsByBlockerAndBlocked(current, candidate)
                && !blockRepository.existsByBlockerAndBlocked(candidate, current);
    }

    private MatchResultDTO buildExploreResult(
            User currentUser,
            User candidate,
            FitnessProfile candidateProfile,
            double score
    ) {
        Match relationship = matchRepository
                .findMatchesBetweenUsers(currentUser.getId(), candidate.getId())
                .stream()
                .findFirst()
                .orElse(null);

        MatchStatus relationshipStatus = relationship != null ? relationship.getStatus() : null;
        String relationshipDirection = null;
        if (relationship != null && relationship.getRequester() != null) {
            relationshipDirection = relationship.getRequester().getId().equals(currentUser.getId())
                    ? "SENT"
                    : "RECEIVED";
        }

        return MatchResultDTO.builder()
                .userId(candidate.getId())
                .fullName(candidate.getName())
                .age(candidate.getAge() != null ? candidate.getAge() : 0)
                .experienceLevel(candidateProfile.getExperienceLevel().name())
                .goal(candidateProfile.getGoal().name())
                .workoutType(candidateProfile.getWorkoutType().name())
                .compatibilityScore(score)
                .profilePictureUrl(candidate.getProfilePictureUrl())
                .gymName(candidate.getGym() != null ? candidate.getGym().getName() : null)
                .bio(candidate.getBio())
                .reliabilityScore(candidate.getReliabilityScore())
                .matchId(relationship != null ? relationship.getId() : null)
                .relationshipStatus(relationshipStatus != null ? relationshipStatus.name() : null)
                .relationshipDirection(relationshipDirection)
                .canSendRequest(canSendExploreRequest(relationshipStatus))
                .build();
    }

    private boolean canSendExploreRequest(MatchStatus relationshipStatus) {
        return relationshipStatus == null
                || relationshipStatus == MatchStatus.REJECTED
                || relationshipStatus == MatchStatus.CANCELLED
                || relationshipStatus == MatchStatus.TERMINATED
                || relationshipStatus == MatchStatus.EXPIRED;
    }

    private boolean matchExists(User u1, User u2, MatchStatus status) {
        return matchRepository.existsByRequesterAndReceiverAndStatus(u1, u2, status)
                || matchRepository.existsByRequesterAndReceiverAndStatus(u2, u1, status);
    }

    private boolean hasActiveMatchAtCurrentGym(User user) {
        if (user.getGym() == null) {
            return false;
        }

        return matchRepository.findAllByUserAndStatus(user, MatchStatus.ACCEPTED)
                .stream()
                .filter(match -> match.getRequester().getRole() != UserRole.ADMIN)
                .filter(match -> match.getReceiver().getRole() != UserRole.ADMIN)
                .anyMatch(match -> match.getGym() != null
                        && match.getGym().getId().equals(user.getGym().getId()));
    }

    public double calculateCompatibility(
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
    private double calculateAgeScore(Integer age1, Integer age2) {

        if (age1 == null || age2 == null) {
            return 5.0; // Default median score for missing age
        }

        int diff = Math.abs(age1 - age2);

        if (diff <= 5) return 10.0;
        if (diff <= 10) return 7.0;
        if (diff <= 15) return 4.0;
        return 1.0;
    }

    public int calculateWeeklyOverlap(
            List<AvailabilitySlot> s1,
            List<AvailabilitySlot> s2) {


        int total = 0;
        if (s1 == null || s2 == null) return 0;

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

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }
}
