package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.RegisterUserRequestDTO;
import com.vinit.gymPartner.dto.RegisterUserResponseDTO;
import com.vinit.gymPartner.dto.UpdateProfileRequest;
import com.vinit.gymPartner.dto.UserResponseDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.UserProfileViewService;
import com.vinit.gymPartner.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileViewService userProfileViewService;

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

    @DeleteMapping("/delete/me")
    public ResponseEntity<String> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        userService.softDeleteUser(userDetails.getUserId());
        return ResponseEntity.ok("Account deleted successfully");
    }


    @GetMapping("/{id}")
    public UserResponseDTO getUserProfile(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User currentUser = userService.getCurrentUserEntity(email);

        userProfileViewService.recordProfileView(
                currentUser.getId(),
                id
        );

        return userService.getUserById(id);
    }
}
