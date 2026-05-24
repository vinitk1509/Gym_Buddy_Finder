package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.BlockedUserDTO;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/block/{blockedId}")
    public ResponseEntity<?> blockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long blockedId
    ) {
        blockService.blockUser(userDetails.getUserId(), blockedId);
        return ResponseEntity.ok("User blocked successfully");
    }

    @DeleteMapping("/unblock/{blockedId}")
    public ResponseEntity<?> unblockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long blockedId
    ) {
        blockService.unblockUser(userDetails.getUserId(), blockedId);
        return ResponseEntity.ok("User unblocked successfully");
    }

    @GetMapping("/blocked-users")
    public ResponseEntity<List<BlockedUserDTO>> getBlockedUsers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(blockService.getBlockedUsers(userDetails.getUserId()));
    }
}
