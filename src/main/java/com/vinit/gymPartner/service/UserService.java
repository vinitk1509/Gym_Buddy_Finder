package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.RegisterUserRequestDTO;
import com.vinit.gymPartner.dto.RegisterUserResponseDTO;
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
}
