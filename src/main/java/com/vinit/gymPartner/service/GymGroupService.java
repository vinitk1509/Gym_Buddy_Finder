package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.CreateGroupSessionRequestDTO;
import com.vinit.gymPartner.dto.GroupChatMessageDTO;
import com.vinit.gymPartner.dto.GroupSessionResponseDTO;
import com.vinit.gymPartner.dto.GymGroupDTO;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.GroupChatMessage;
import com.vinit.gymPartner.entity.GroupWorkoutSession;
import com.vinit.gymPartner.entity.GymGroup;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.SessionState;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.repository.AvailabilitySlotRepository;
import com.vinit.gymPartner.repository.GroupChatMessageRepository;
import com.vinit.gymPartner.repository.GroupWorkoutSessionRepository;
import com.vinit.gymPartner.repository.GymGroupRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GymGroupService {

    private final GymGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final GroupWorkoutSessionRepository groupWorkoutSessionRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final MatchRepository matchRepository;

    public GymGroupDTO createGroup(Long creatorId, String name, String description, int targetCapacity) {
        User creator = findUser(creatorId);
        if (creator.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Admin accounts cannot create gym groups");
        }
        if (creator.getGym() == null) {
            throw new RuntimeException("Set your gym before creating a group");
        }

        GymGroup group = GymGroup.builder()
                .name(validateName(name))
                .description(description != null ? description.trim() : null)
                .creator(creator)
                .gym(creator.getGym())
                .targetCapacity(validateTargetCapacity(targetCapacity, creator))
                .createdAt(LocalDateTime.now())
                .build();

        group.getMembers().add(creator);
        return toDTO(groupRepository.save(group), creatorId);
    }

    public GymGroupDTO joinGroup(Long userId, Long groupId) {
        User user = findUser(userId);
        GymGroup group = findGroup(groupId);
        assertNotBanned(group);
        addMember(group, user);
        return toDTO(groupRepository.save(group), userId);
    }

    public GymGroupDTO addMember(Long creatorId, Long groupId, Long userId) {
        GymGroup group = findGroup(groupId);
        assertNotBanned(group);
        User actor = findUser(creatorId);
        if (!isGroupMember(group, creatorId)) {
            throw new RuntimeException("Only group members can add members");
        }
        User user = findUser(userId);
        if (!hasAcceptedMatch(actor, user)) {
            throw new RuntimeException("You can add only people you are matched with");
        }
        addMember(group, user);
        return toDTO(groupRepository.save(group), creatorId);
    }

    public void deleteGroup(Long userId, Long groupId) {
        GymGroup group = findGroup(groupId);
        if (!group.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the group creator can delete this group for everyone");
        }
        groupChatMessageRepository.deleteByGroupId(group.getId());
        groupWorkoutSessionRepository.deleteByGroupId(group.getId());
        group.getMembers().clear();
        groupRepository.delete(group);
    }

    public GymGroupDTO banGroup(Long adminId, Long groupId) {
        User admin = findUser(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Only admin can ban groups");
        }
        GymGroup group = findGroup(groupId);
        group.setBanned(true);
        group.setBannedAt(LocalDateTime.now());
        group.setBannedBy(admin);
        return toDTO(groupRepository.save(group), adminId);
    }

    public void leaveGroup(Long userId, Long groupId) {
        GymGroup group = findGroup(groupId);
        if (group.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Group creator cannot leave their own group. Delete it for everyone instead.");
        }
        boolean removed = group.getMembers().removeIf(member -> member.getId().equals(userId));
        if (!removed) {
            throw new RuntimeException("You are not a member of this group");
        }
        groupRepository.save(group);
    }

    public List<GymGroupDTO> getGroupsForMyGym(Long userId) {
        User user = findUser(userId);
        if (user.getRole() == UserRole.ADMIN) {
            return groupRepository.findAll()
                    .stream()
                    .map(group -> toDTO(group, userId))
                    .toList();
        }
        if (user.getGym() == null) {
            return List.of();
        }
        return groupRepository.findByMembersId(userId)
                .stream()
                .map(group -> toDTO(group, userId))
                .toList();
    }

    public List<GroupChatMessageDTO> getMessages(Long userId, Long groupId) {
        assertMemberOrAdmin(userId, groupId);
        return groupChatMessageRepository.findByGroupIdOrderBySentAtAsc(groupId)
                .stream()
                .map(this::toMessageDTO)
                .toList();
    }

    public GroupChatMessageDTO sendMessage(Long userId, Long groupId, String content) {
        GymGroup group = assertMember(userId, groupId);
        assertNotBanned(group);
        User sender = findUser(userId);
        String cleanContent = validateMessage(content);

        GroupChatMessage message = GroupChatMessage.builder()
                .group(group)
                .sender(sender)
                .content(cleanContent)
                .sentAt(LocalDateTime.now())
                .build();

        return toMessageDTO(groupChatMessageRepository.save(message));
    }

    public GroupSessionResponseDTO createGroupSession(Long userId, CreateGroupSessionRequestDTO request) {
        GymGroup group = assertMember(userId, request.getGroupId());
        assertNotBanned(group);
        User creator = findUser(userId);
        LocalDateTime start = request.getStart();
        LocalDateTime end = request.getEnd();

        if (start == null || end == null || !end.isAfter(start)) {
            throw new RuntimeException("Invalid session time");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Session must be in future");
        }
        if (!groupWorkoutSessionRepository.findOverlappingSessions(
                group.getId(),
                start,
                end,
                List.of(SessionState.PENDING_APPROVAL, SessionState.SCHEDULED)
        ).isEmpty()) {
            throw new RuntimeException("Group already has a session at this time");
        }
        if (!allMembersAvailable(group, start, end)) {
            throw new RuntimeException("All group members must be available for that time");
        }
        if (hasAnyMemberOverlap(group, start, end)) {
            throw new RuntimeException("One or more members already has a session at this time");
        }

        GroupWorkoutSession session = GroupWorkoutSession.builder()
                .group(group)
                .gym(group.getGym())
                .createdBy(creator)
                .startDateTime(start)
                .endDateTime(end)
                .state(SessionState.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .build();

        return toSessionDTO(groupWorkoutSessionRepository.save(session));
    }

    public List<GroupSessionResponseDTO> getMyGroupSessions(Long userId) {
        return groupWorkoutSessionRepository.findByMemberId(userId)
                .stream()
                .map(this::toSessionDTO)
                .toList();
    }

    private void addMember(GymGroup group, User user) {
        assertNotBanned(group);
        if (isGroupMember(group, user.getId())) {
            return;
        }
        if (user.getGym() == null || !user.getGym().getId().equals(group.getGym().getId())) {
            throw new RuntimeException("Members must belong to the same gym");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Admin accounts cannot join workout groups");
        }
        if (group.getMembers().size() >= group.getTargetCapacity()) {
            throw new RuntimeException("Group is full");
        }
        validateMemberPreferences(group, user);
        group.getMembers().add(user);
    }

    private int validateTargetCapacity(int targetCapacity, User creator) {
        int creatorLimit = normalizedTargetGroupSize(creator);
        int normalized = Math.max(2, Math.min(8, targetCapacity));
        if (normalized > creatorLimit) {
            throw new RuntimeException("Group size cannot be bigger than your profile group-size preference");
        }
        return normalized;
    }

    private void validateMemberPreferences(GymGroup group, User newMember) {
        int nextSize = group.getMembers().size() + 1;
        if (normalizedTargetGroupSize(newMember) < nextSize) {
            throw new RuntimeException("This user is not looking for a group this large");
        }
        boolean existingMemberBlocked = group.getMembers().stream()
                .anyMatch(member -> normalizedTargetGroupSize(member) < nextSize);
        if (existingMemberBlocked) {
            throw new RuntimeException("One or more members is not looking for a group this large");
        }
    }

    private int normalizedTargetGroupSize(User user) {
        return user.getTargetGroupSize() != null ? user.getTargetGroupSize() : 1;
    }

    private boolean hasAcceptedMatch(User user1, User user2) {
        return matchRepository.existsByRequesterAndReceiverAndStatus(user1, user2, MatchStatus.ACCEPTED)
                || matchRepository.existsByRequesterAndReceiverAndStatus(user2, user1, MatchStatus.ACCEPTED);
    }

    private boolean hasAnyMemberOverlap(GymGroup group, LocalDateTime start, LocalDateTime end) {
        List<SessionState> activeStates = List.of(SessionState.PENDING_APPROVAL, SessionState.SCHEDULED);
        return group.getMembers().stream().anyMatch(member ->
                !workoutSessionRepository.findOverlappingSessionsForUser(member.getId(), start, end).isEmpty()
                        || !groupWorkoutSessionRepository.findOverlappingSessionsForUser(
                                member.getId(), start, end, activeStates
                        ).isEmpty()
        );
    }

    private boolean allMembersAvailable(GymGroup group, LocalDateTime start, LocalDateTime end) {
        return group.getMembers().stream().allMatch(member ->
                availabilitySlotRepository.findByUserId(member.getId())
                        .stream()
                        .anyMatch(slot -> covers(slot, start, end))
        );
    }

    private boolean covers(AvailabilitySlot slot, LocalDateTime start, LocalDateTime end) {
        return slot.getDayOfWeek() == start.getDayOfWeek()
                && !slot.getStartTime().isAfter(start.toLocalTime())
                && !slot.getEndTime().isBefore(end.toLocalTime());
    }

    private GymGroup assertMember(Long userId, Long groupId) {
        GymGroup group = findGroup(groupId);
        if (!isGroupMember(group, userId)) {
            throw new RuntimeException("You are not a member of this group");
        }
        return group;
    }

    private void assertNotBanned(GymGroup group) {
        if (Boolean.TRUE.equals(group.getBanned())) {
            throw new RuntimeException("This group was banned by admin");
        }
    }

    private GymGroup assertMemberOrAdmin(Long userId, Long groupId) {
        GymGroup group = findGroup(groupId);
        User user = findUser(userId);
        if (user.getRole() != UserRole.ADMIN && !isGroupMember(group, userId)) {
            throw new RuntimeException("You are not a member of this group");
        }
        return group;
    }

    private boolean isGroupMember(GymGroup group, Long userId) {
        return group.getMembers().stream().anyMatch(member -> member.getId().equals(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private GymGroup findGroup(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private String validateName(String name) {
        if (name == null || name.trim().length() < 3) {
            throw new RuntimeException("Group name must be at least 3 characters");
        }
        return name.trim();
    }

    private String validateMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }
        if (content.length() > 1000) {
            throw new RuntimeException("Message is too long");
        }
        return content.trim();
    }

    private GymGroupDTO toDTO(GymGroup group, Long currentUserId) {
        List<GymGroupDTO.MemberDTO> members = group.getMembers().stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .map(member -> GymGroupDTO.MemberDTO.builder()
                        .userId(member.getId())
                        .name(member.getName())
                        .profilePictureUrl(member.getProfilePictureUrl())
                        .reliabilityScore(member.getReliabilityScore())
                        .age(member.getAge())
                        .gymName(member.getGym() != null ? member.getGym().getName() : null)
                        .fitnessGoal(member.getFitnessProfile() != null && member.getFitnessProfile().getGoal() != null
                                ? member.getFitnessProfile().getGoal().name() : null)
                        .workoutType(member.getFitnessProfile() != null && member.getFitnessProfile().getWorkoutType() != null
                                ? member.getFitnessProfile().getWorkoutType().name() : null)
                        .experienceLevel(member.getFitnessProfile() != null && member.getFitnessProfile().getExperienceLevel() != null
                                ? member.getFitnessProfile().getExperienceLevel().name() : null)
                        .bio(member.getBio())
                        .targetGroupSize(member.getTargetGroupSize())
                        .build())
                .toList();

        return GymGroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .creatorId(group.getCreator().getId())
                .creatorName(group.getCreator().getName())
                .gymName(group.getGym() != null ? group.getGym().getName() : null)
                .targetCapacity(group.getTargetCapacity())
                .memberCount(group.getMembers().size())
                .currentUserMember(group.getMembers().stream().anyMatch(member -> member.getId().equals(currentUserId)))
                .currentUserCreator(group.getCreator().getId().equals(currentUserId))
                .banned(Boolean.TRUE.equals(group.getBanned()))
                .bannedById(group.getBannedBy() != null ? group.getBannedBy().getId() : null)
                .bannedByName(group.getBannedBy() != null ? group.getBannedBy().getName() : null)
                .bannedAt(group.getBannedAt())
                .createdAt(group.getCreatedAt())
                .members(members)
                .build();
    }

    private GroupChatMessageDTO toMessageDTO(GroupChatMessage message) {
        User sender = message.getSender();
        return GroupChatMessageDTO.builder()
                .id(message.getId())
                .groupId(message.getGroup().getId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .senderProfilePictureUrl(sender.getProfilePictureUrl())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }

    private GroupSessionResponseDTO toSessionDTO(GroupWorkoutSession session) {
        return GroupSessionResponseDTO.builder()
                .id(session.getId())
                .groupId(session.getGroup().getId())
                .groupName(session.getGroup().getName())
                .startDateTime(session.getStartDateTime())
                .endDateTime(session.getEndDateTime())
                .state(session.getState().name())
                .createdById(session.getCreatedBy().getId())
                .createdByName(session.getCreatedBy().getName())
                .build();
    }
}
