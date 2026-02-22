package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<MatchResultDTO>> getMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        if (page < 0 || size <= 0 || size > 50) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        // 🔐 Extract logged-in user from JWT
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "compatibilityScore")
        );

        Page<MatchResultDTO> matches =
                matchingService.findCompatibleUsers(user.getId(), pageable);

        return ResponseEntity.ok(matches);
    }
}