package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.ChatPreviewDTO;
import com.vinit.gymPartner.dto.ChatMessageDTO;
import com.vinit.gymPartner.entity.ChatEmailNotification;
import com.vinit.gymPartner.entity.ChatMessage;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.repository.ChatEmailNotificationRepository;
import com.vinit.gymPartner.repository.ChatMessageRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private static final int CHAT_EMAIL_COOLDOWN_HOURS = 24;
    public static final String ADMIN_DISPLAY_NAME = "ADMIN-gymPartner";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatEmailNotificationRepository chatEmailNotificationRepository;
    private final MatchRepository matchRepository;
    private final EmailService emailService;

    @Transactional
    public ChatMessage sendMessage(ChatMessageDTO dto) {
        return sendMessageAsUser(dto, dto.getSenderId());
    }

    @Transactional
    public ChatMessage sendMessageAsUser(ChatMessageDTO dto, Long authenticatedUserId) {
        Match match = matchRepository.findById(dto.getMatchId())
                .orElseThrow(()->new RuntimeException("Match not found"));

        if(match.getStatus() != MatchStatus.ACCEPTED){
            throw new RuntimeException("Chat is only available for accepted matches");
        }

        boolean isSender = match.getRequester().getId().equals(dto.getSenderId())
                || match.getReceiver().getId().equals(dto.getSenderId());
        if(!isSender){
            throw new RuntimeException("You are not part of this match");
        }
        User receiver = match.getRequester().getId().equals(authenticatedUserId)
                ? match.getReceiver()
                : match.getRequester();

        ChatMessage message = ChatMessage.builder()
                .matchId(dto.getMatchId())
                .senderId(authenticatedUserId)
                .receiverId(receiver.getId())
                .content(validateContent(dto.getContent()))
                .sentAt(LocalDateTime.now())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Eagerly resolve lazy-loaded names within the transaction
        User sender = match.getRequester().getId().equals(authenticatedUserId)
                ? match.getRequester()
                : match.getReceiver();
        String senderName = displayName(sender);

        log.info("Chat message saved (id={}) from user {} to user {} in match {}",
                savedMessage.getId(), savedMessage.getSenderId(), savedMessage.getReceiverId(), dto.getMatchId());

        maybeSendChatEmail(match, savedMessage, senderName);

        return savedMessage;
    }

    public List<ChatMessage> getChatHistory(Long matchId, Long userId) {
        assertUserCanAccessMatch(matchId, userId);
        return chatMessageRepository.findByMatchIdOrderBySentAtAsc(matchId);
    }

    public List<ChatMessage> getChatHistory(Long matchId){
        return chatMessageRepository.findByMatchIdOrderBySentAtAsc(matchId);
    }

    public List<ChatPreviewDTO> getChatPreviews(Long userId) {
        return matchRepository.findByRequesterIdOrReceiverId(userId, userId)
                .stream()
                .filter(match -> match.getStatus() == MatchStatus.ACCEPTED)
                .map(match -> buildPreview(match, userId))
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByReceiverIdAndReadFalse(userId);
    }

    public long getUnreadCountForMatch(Long userId, Long matchId) {
        assertUserCanAccessMatch(matchId, userId);
        return chatMessageRepository.countByReceiverIdAndReadFalseAndMatchId(userId, matchId);
    }

    @Transactional
    public void markMatchRead(Long matchId, Long userId) {
        assertUserCanAccessMatch(matchId, userId);
        chatMessageRepository.markUnreadMessagesRead(matchId, userId);
    }

    private void maybeSendChatEmail(Match match, ChatMessage message, String senderName) {
        User receiver = match.getRequester().getId().equals(message.getReceiverId())
                ? match.getRequester()
                : match.getReceiver();
        String receiverEmail = receiver.getEmail();
        String receiverName = receiver.getName();
        LocalDateTime now = LocalDateTime.now();
        ChatEmailNotification notification = chatEmailNotificationRepository
                .findByMatchIdAndSenderIdAndReceiverId(message.getMatchId(), message.getSenderId(), message.getReceiverId())
                .orElse(null);

        if (notification != null
                && notification.getLastSentAt().isAfter(now.minusHours(CHAT_EMAIL_COOLDOWN_HOURS))) {
            log.debug("Skipping chat email for match {} from user {} to user {}; cooldown still active",
                    message.getMatchId(), message.getSenderId(), message.getReceiverId());
            return;
        }

        if (notification == null) {
            notification = ChatEmailNotification.builder()
                    .matchId(message.getMatchId())
                    .senderId(message.getSenderId())
                    .receiverId(message.getReceiverId())
                    .build();
        }
        notification.setLastSentAt(now);
        chatEmailNotificationRepository.save(notification);

        emailService.sendChatMessageEmail(receiverEmail, receiverName, senderName, message.getContent());
    }

    private ChatPreviewDTO buildPreview(Match match, Long userId) {
        User partner = match.getRequester().getId().equals(userId)
                ? match.getReceiver()
                : match.getRequester();
        ChatMessage lastMessage = chatMessageRepository.findTopByMatchIdOrderBySentAtDesc(match.getId())
                .orElse(null);

        return ChatPreviewDTO.builder()
                .matchId(match.getId())
                .partnerId(partner.getId())
                .partnerName(displayName(partner))
                .partnerProfilePicture(partner.getProfilePictureUrl())
                .partnerActiveNow(partner.getLastSeenAt() != null
                        && partner.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(5)))
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getSentAt() : null)
                .unreadCount(chatMessageRepository.countByReceiverIdAndReadFalseAndMatchId(userId, match.getId()))
                .build();
    }

    public static String displayName(User user) {
        return user.getRole() == UserRole.ADMIN ? ADMIN_DISPLAY_NAME : user.getName();
    }

    private void assertUserCanAccessMatch(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getRequester().getId().equals(userId)
                && !match.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("You are not part of this match");
        }
    }

    private String validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }
        if (content.length() > 1000) {
            throw new RuntimeException("Message is too long");
        }
        return content.trim();
    }
}
