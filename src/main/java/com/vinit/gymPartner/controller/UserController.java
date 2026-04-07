package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.RegisterUserRequestDTO;
import com.vinit.gymPartner.dto.RegisterUserResponseDTO;
import com.vinit.gymPartner.dto.UpdateProfileRequest;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.service.FileStorageService;
import com.vinit.gymPartner.dto.UserResponseDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.service.UserService;
import com.vinit.gymPartner.entity.DeviceToken;
import com.vinit.gymPartner.repository.DeviceTokenRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final DeviceTokenRepository deviceTokenRepository;
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

        return  ResponseEntity.ok(userService.getCurrentUser(email));
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
            @RequestParam("file")MultipartFile file,
            Authentication authentication){
        String contentType = file.getContentType();

        if(contentType == null || !contentType.startsWith("image/")){
            return ResponseEntity.badRequest().body("Only image files are allowed");
        }

        if(file.getSize() > 5 * 1024 * 1024){
            return ResponseEntity.badRequest().body("File size must be under 5MB");
        }

        String imageUrl = fileStorageService.storeFile(file);

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setProfilePictureUrl(imageUrl);
        userRepository.save(user);
        return ResponseEntity.ok(imageUrl);
    }
    @PostMapping("/device-token")
    public ResponseEntity<String> registerDeviceToken(
            @RequestParam String token,
            @RequestParam(defaultValue = "WEB") String platform,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Avoid duplicate tokens (same device registering twice)
        if (deviceTokenRepository.findByToken(token).isEmpty()) {
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .token(token)
                    .platform(platform)
                    .build();
            deviceTokenRepository.save(deviceToken);
        }

        return ResponseEntity.ok("Device token registered");
    }
}
