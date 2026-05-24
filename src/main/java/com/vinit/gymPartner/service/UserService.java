package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.RegisterUserRequestDTO;
import com.vinit.gymPartner.dto.RegisterUserResponseDTO;
import com.vinit.gymPartner.dto.UpdateProfileRequest;
import com.vinit.gymPartner.dto.UserResponseDTO;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.Gym;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.ExperienceLevel;
import com.vinit.gymPartner.entity.enums.FitnessGoal;
import com.vinit.gymPartner.entity.enums.Gender;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.entity.enums.WorkoutType;
import com.vinit.gymPartner.repository.FitnessProfileRepository;
import com.vinit.gymPartner.repository.GymRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.repository.WorkoutSessionRepository;
import com.vinit.gymPartner.entity.WorkoutSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final FitnessProfileRepository fitnessProfileRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final MatchRepository matchRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public static final int DEFAULT_RELIABILITY_SCORE = 100;

    // Reliability score deltas
    public static final int COMPLETED_SESSION_BONUS = 5;
    public static final int NO_SHOW_PENALTY = -15;
    public static final int LATE_CANCEL_PENALTY = -10;
    public static final int REPORT_WARNING_PENALTY = -20;
    public static final int WEEKLY_STREAK_BONUS = 10;

    public RegisterUserResponseDTO registerUser(RegisterUserRequestDTO requestDTO){
        String normalizedEmail = normalizeEmail(requestDTO.getEmail());
        requestDTO.setEmail(normalizedEmail);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)){
            throw new RuntimeException("Email Already exist");
        }

        int age = calculateAge(requestDTO.getDateOfBirth());
        if (age < 16) {
            throw new RuntimeException("You must be at least 16 years old to register.");
        }

        if (!emailService.verifyCode(normalizedEmail, requestDTO.getEmailVerificationCode())) {
            throw new RuntimeException("Please verify your email before continuing.");
        }

        Gym gym = null;
        if (requestDTO.getPlaceId() != null && !requestDTO.getPlaceId().isBlank()) {
            gym = gymRepository.findByPlaceId(requestDTO.getPlaceId()).orElse(null);
        }
        if (gym == null) {
            gym = gymRepository.findByNameAndAddress(requestDTO.getGymName(), requestDTO.getGymAddress())
                .orElse(null);
        }
        
        if (gym == null) {
            Gym newGym = new Gym();
            newGym.setName(requestDTO.getGymName());
            newGym.setAddress(requestDTO.getGymAddress());
            newGym.setPlaceId(requestDTO.getPlaceId());
            newGym.setTimezone(TimeZone.getDefault().getID());
            if (requestDTO.getLatitude() != null) {
                newGym.setLatitude(requestDTO.getLatitude());
            }
            if (requestDTO.getLongitude() != null) {
                newGym.setLongitude(requestDTO.getLongitude());
            }
            gym = gymRepository.save(newGym);
        }

        User user = new User();
        user.setName(requestDTO.getFullName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setGym(gym);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        user.setLookingForPartner(true);
        user.setAllowMultiplePartners(false);
        user.setTargetGroupSize(1);
        user.setReliabilityScore(DEFAULT_RELIABILITY_SCORE);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        user.setDateOfBirth(requestDTO.getDateOfBirth());
        user.setAge(age);

        // Set bio
        if (requestDTO.getBio() != null && !requestDTO.getBio().isBlank()) {
            user.setBio(requestDTO.getBio());
        }

        // Set gender
        if (requestDTO.getGender() != null && !requestDTO.getGender().isBlank()) {
            user.setGender(Gender.valueOf(requestDTO.getGender().trim()));
        }

        userRepository.save(user);

        FitnessProfile profile = new FitnessProfile();
        profile.setUser(user);
        profile.setExperienceLevel(
                ExperienceLevel.valueOf(requestDTO.getExperienceLevel()));
        profile.setGoal(
                FitnessGoal.valueOf(requestDTO.getGoal()));
        profile.setWorkoutType(
                WorkoutType.valueOf(requestDTO.getWorkoutType()));

        fitnessProfileRepository.save(profile);

        return RegisterUserResponseDTO.builder()
                .userId(user.getId())
                .fullName(user.getName())
                .email(user.getEmail())
                .message("User Registered Successfully")
                .build();
    }

    @Transactional
    public UserResponseDTO getCurrentUser(String email)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User Not Found"));
        return convertToResponseDTO(user);
    }

    public UserResponseDTO convertToResponseDTO(User user) {
        FitnessProfile profile = user.getFitnessProfile();
        long totalSessions = workoutSessionRepository.countMeaningfulSessionsByUserId(user.getId());
        long completedSessions = workoutSessionRepository.countCompletedSessionsByUserId(user.getId());
        
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .gymName(user.getGym() != null ? user.getGym().getName() : null)
                .gymAddress(user.getGym() != null ? user.getGym().getAddress() : null)
                .role(user.getRole())
                .status(user.getStatus())
                .reliabilityScore(user.getReliabilityScore())
                .allowMultiplePartners(user.getAllowMultiplePartners())
                .lookingForPartner(user.isLookingForPartner())
                .targetGroupSize(user.getTargetGroupSize())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .experienceLevel(profile != null ? profile.getExperienceLevel().toString() : null)
                .fitnessGoal(profile != null ? profile.getGoal().toString() : null)
                .workoutType(profile != null ? profile.getWorkoutType().toString() : null)
                .profilePictureUrl(user.getProfilePictureUrl())
                .bio(user.getBio())
                .age(resolveAge(user))
                .dateOfBirth(user.getDateOfBirth())
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .currentStreakDays(calculateCurrentStreakDays(user.getId()))
                .build();
    }

    private int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now())) {
            throw new RuntimeException("Please enter a valid date of birth.");
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private Integer resolveAge(User user) {
        if (user.getDateOfBirth() != null) {
            int currentAge = calculateAge(user.getDateOfBirth());
            if (!Integer.valueOf(currentAge).equals(user.getAge())) {
                user.setAge(currentAge);
                userRepository.save(user);
            }
            return currentAge;
        }
        return user.getAge();
    }

    private int calculateCurrentStreakDays(Long userId) {
        Set<LocalDate> completedDates = workoutSessionRepository.findCompletedSessionsByUserId(userId)
                .stream()
                .map(WorkoutSession::getStartDateTime)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        if (completedDates.isEmpty()) {
            return 0;
        }

        LocalDate cursor = LocalDate.now();
        if (!completedDates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }

        int streak = 0;
        while (completedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    public User updateCurrentUser(String email, UpdateProfileRequest request)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User Not Found"));

        if (request.getLookingForPartner() != null) {
            user.setLookingForPartner(request.getLookingForPartner());
        }

        if (request.getAllowMultiplePartners() != null) {
            user.setAllowMultiplePartners(request.getAllowMultiplePartners());
        }

        if (request.getTargetGroupSize() != null) {
            int targetGroupSize = Math.max(1, Math.min(8, request.getTargetGroupSize()));
            user.setTargetGroupSize(targetGroupSize);
            user.setAllowMultiplePartners(targetGroupSize > 1 || Boolean.TRUE.equals(user.getAllowMultiplePartners()));
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        // Update gym if provided
        if (request.getGymName() != null && request.getGymAddress() != null) {
            Gym gym = null;
            if (request.getPlaceId() != null && !request.getPlaceId().isBlank()) {
                gym = gymRepository.findByPlaceId(request.getPlaceId()).orElse(null);
            }
            if (gym == null) {
                gym = gymRepository.findByNameAndAddress(request.getGymName(), request.getGymAddress())
                    .orElse(null);
            }

            if (gym == null) {
                Gym newGym = new Gym();
                newGym.setName(request.getGymName());
                newGym.setAddress(request.getGymAddress());
                newGym.setPlaceId(request.getPlaceId());
                newGym.setTimezone(TimeZone.getDefault().getID());
                if (request.getLatitude() != null) newGym.setLatitude(request.getLatitude());
                if (request.getLongitude() != null) newGym.setLongitude(request.getLongitude());
                gym = gymRepository.save(newGym);
            }
            Gym oldGym = user.getGym();
            boolean gymChanged = oldGym != null
                    && gym.getId() != null
                    && !oldGym.getId().equals(gym.getId());

            if (gymChanged) {
                cancelPendingMatchesFromPreviousGym(user, oldGym);
            }

            user.setGym(gym);
        }

        // Update fitness profile if goal, experience level, or workout type is provided
        FitnessProfile profile = user.getFitnessProfile();
        if (profile != null) {
            if (request.getFitnessGoal() != null) {
                profile.setGoal(FitnessGoal.valueOf(request.getFitnessGoal()));
            }
            if (request.getExperienceLevel() != null) {
                profile.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel()));
            }
            if (request.getWorkoutType() != null) {
                profile.setWorkoutType(WorkoutType.valueOf(request.getWorkoutType()));
            }
            fitnessProfileRepository.save(profile);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }

    private void cancelPendingMatchesFromPreviousGym(User user, Gym oldGym) {
        List<Match> pendingMatches = matchRepository.findPendingByUserAndGym(user, oldGym);
        LocalDateTime now = LocalDateTime.now();

        for (Match match : pendingMatches) {
            match.setStatus(MatchStatus.CANCELLED);
            match.setUpdatedAt(now);
        }

        if (!pendingMatches.isEmpty()) {
            matchRepository.saveAll(pendingMatches);
        }
    }

    public void updateReliability(User user, int delta) {
        int newScore = Math.max(0, Math.min(100, user.getReliabilityScore() + delta));
        user.setReliabilityScore(newScore);
        userRepository.save(user);
    }

    /**
     * Soft-delete: mark user as INACTIVE and set deletion request timestamp.
     */
    public void requestAccountDeletion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        user.setStatus(UserStatus.INACTIVE);
        user.setDeletionRequestedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Update lastLoginAt timestamp.
     */
    public void recordLogin(User user) {
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setLastSeenAt(now);
        userRepository.save(user);
    }

    public void recordActivity(User user) {
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void recordLogout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        user.setLastSeenAt(null);
        userRepository.save(user);
    }

    /**
     * Reset password for a user.
     */
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
