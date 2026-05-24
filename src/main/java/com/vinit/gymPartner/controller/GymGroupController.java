package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.CreateGroupSessionRequestDTO;
import com.vinit.gymPartner.dto.GroupChatMessageDTO;
import com.vinit.gymPartner.dto.GroupSessionResponseDTO;
import com.vinit.gymPartner.dto.GymGroupDTO;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.GymGroupService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GymGroupController {

    private final GymGroupService groupService;

    @GetMapping
    public ResponseEntity<List<GymGroupDTO>> getGymGroups(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(groupService.getGroupsForMyGym(userDetails.getUserId()));
    }

    @PostMapping
    public ResponseEntity<GymGroupDTO> createGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateGroupRequest request
    ) {
        return ResponseEntity.ok(groupService.createGroup(
                userDetails.getUserId(),
                request.getName(),
                request.getDescription(),
                request.getTargetCapacity()
        ));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<GymGroupDTO> joinGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.joinGroup(userDetails.getUserId(), groupId));
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<GymGroupDTO> addMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(groupService.addMember(userDetails.getUserId(), groupId, userId));
    }

    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<Void> leaveGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId
    ) {
        groupService.leaveGroup(userDetails.getUserId(), groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId
    ) {
        groupService.deleteGroup(userDetails.getUserId(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/ban")
    public ResponseEntity<GymGroupDTO> banGroup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.banGroup(userDetails.getUserId(), groupId));
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<GroupChatMessageDTO>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.getMessages(userDetails.getUserId(), groupId));
    }

    @PostMapping("/{groupId}/messages")
    public ResponseEntity<GroupChatMessageDTO> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId,
            @RequestBody SendGroupMessageRequest request
    ) {
        return ResponseEntity.ok(groupService.sendMessage(userDetails.getUserId(), groupId, request.getContent()));
    }

    @GetMapping("/sessions/my")
    public ResponseEntity<List<GroupSessionResponseDTO>> getMyGroupSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(groupService.getMyGroupSessions(userDetails.getUserId()));
    }

    @PostMapping("/{groupId}/sessions")
    public ResponseEntity<GroupSessionResponseDTO> createGroupSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long groupId,
            @RequestBody CreateGroupSessionRequestDTO request
    ) {
        request.setGroupId(groupId);
        return ResponseEntity.ok(groupService.createGroupSession(userDetails.getUserId(), request));
    }

    @Data
    public static class CreateGroupRequest {
        private String name;
        private String description;
        private int targetCapacity;
    }

    @Data
    public static class SendGroupMessageRequest {
        private String content;
    }
}
