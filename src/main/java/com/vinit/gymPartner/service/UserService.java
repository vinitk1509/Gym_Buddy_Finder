package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.RegisterUserRequestDTO;
import com.vinit.gymPartner.dto.RegisterUserResponseDTO;
import com.vinit.gymPartner.dto.UpdateProfileRequest;
import com.vinit.gymPartner.dto.UserResponseDTO;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.Gym;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.ExperienceLevel;
import com.vinit.gymPartner.entity.enums.FitnessGoal;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.entity.enums.WorkoutType;
import com.vinit.gymPartner.repository.FitnessProfileRepository;
import com.vinit.gymPartner.repository.GymRepository;
import com.vinit.gymPartner.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final FitnessProfileRepository fitnessProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserResponseDTO registerUser(RegisterUserRequestDTO requestDTO){
        if (userRepository.existsByEmail(requestDTO.getEmail())){
            throw new RuntimeException("Email Already exist");
        }

        Gym gym = gymRepository
                .findByNameAndAddress(
                        requestDTO.getGymName(),
                        requestDTO.getGymAddress()
                )
                .orElseGet(() -> {
                    Gym newGym = new Gym();
                    newGym.setName(requestDTO.getGymName());
                    newGym.setAddress(requestDTO.getGymAddress());
                    newGym.setTimezone("UTC");
                    return gymRepository.save(newGym);
                });

        User user = new User();
        user.setName(requestDTO.getFullName());
        user.setEmail(requestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setGym(gym);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        user.setLookingForPartner(true);
        user.setAllowMultiplePartners(false);
        user.setReliabilityScore(100);

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
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .experienceLevel(profile != null ? profile.getExperienceLevel().toString() : null)
                .fitnessGoal(profile != null ? profile.getGoal().toString() : null)
                .workoutType(profile != null ? profile.getWorkoutType().toString() : null)
                .build();
    }

    public User updateCurrentUser(String email, UpdateProfileRequest request)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User Not Found"));

        user.setLookingForPartner(request.isLookingForPartner());

        // Update fitness profile if goal or experience level is provided
        if (request.getFitnessGoal() != null || request.getExperienceLevel() != null) {
            FitnessProfile profile = user.getFitnessProfile();
            
            if (request.getFitnessGoal() != null) {
                profile.setGoal(FitnessGoal.valueOf(request.getFitnessGoal()));
            }
            if (request.getExperienceLevel() != null) {
                profile.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel()));
            }
            
            fitnessProfileRepository.save(profile);
        }

        userRepository.save(user);
        return user;
    }
}
