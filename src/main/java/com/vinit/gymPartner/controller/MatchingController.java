package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.MatchResultDTO;
import com.vinit.gymPartner.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @GetMapping("/{userId}")
    public ResponseEntity<Page<MatchResultDTO>> getMatches(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ){
        if (page < 0 || size <= 0 || size > 50) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "compatibilityScore")
        );

        Page<MatchResultDTO> matches =
                matchingService.findCompatibleUsers(userId, pageable);

        return ResponseEntity.ok(matches);
    }
}




