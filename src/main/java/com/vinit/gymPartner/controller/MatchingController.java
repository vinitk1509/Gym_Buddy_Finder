package com.vinit.gymPartner.controller;


import com.vinit.gymPartner.dto.ExploreFilterDTO;
import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.dto.NearbyUserDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.LocationService;
import com.vinit.gymPartner.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class MatchingController {

    public final MatchingService matchingService;
    public final UserRepository userRepository;
    public LocationService locationService;

    @GetMapping
    public ResponseEntity<List<MatchResultDTO>> getSuggestions(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        return ResponseEntity.ok(
                matchingService.findCompatibleUsers(userDetails.getUserId())
        );
    }

    @GetMapping("/explore")
    public ResponseEntity<List<MatchResultDTO>> explore(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute ExploreFilterDTO filter
            ){
        return ResponseEntity.ok(
                matchingService.findExploreUsers(
                        userDetails.getUserId(),
                        filter)
        );
    }
    // Add this endpoint to MatchingController.java:

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyUserDTO>> getNearbyUsers(
            @RequestParam(defaultValue = "10") double radiusKm,
            Authentication authentication) {

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(locationService.findNearbyUsers(currentUser, radiusKm));
    }


}