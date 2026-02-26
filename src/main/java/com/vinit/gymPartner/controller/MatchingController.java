package com.vinit.gymPartner.controller;


import com.vinit.gymPartner.dto.ExploreFilterDTO;
import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class MatchingController {

    public final MatchingService matchingService;

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

}