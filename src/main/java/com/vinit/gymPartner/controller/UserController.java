package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.RegisterUserRequestDTO;
import com.vinit.gymPartner.dto.RegisterUserResponseDTO;
import com.vinit.gymPartner.dto.UpdateProfileRequest;
import com.vinit.gymPartner.dto.UserResponseDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.service.FileStorageService;
import com.vinit.gymPartner.service.UserService;
import com.vinit.gymPartner.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponseDTO> register(
            @Valid @RequestBody RegisterUserRequestDTO request
            ){
        RegisterUserResponseDTO response = userService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userService.recordActivity(user);

        return  ResponseEntity.ok(userService.getCurrentUser(email));
    }

    @PostMapping("/activity")
    public ResponseEntity<Void> recordActivity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userService.recordActivity(user);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/me")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @RequestBody UpdateProfileRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User updatedUser = userService.updateCurrentUser(email, request);
        return ResponseEntity.ok(userService.convertToResponseDTO(updatedUser));
    }

    @PostMapping("/profile/picture")
    public ResponseEntity<String> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fileUrl = fileStorageService.storeFile(file);
        user.setProfilePictureUrl(fileUrl);
        userRepository.save(user);

        return ResponseEntity.ok(fileUrl);
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        userService.requestAccountDeletion(email);

        return ResponseEntity.ok("Account deletion requested. Your account is now inactive and will be permanently deleted after 30 days.");
    }
}
