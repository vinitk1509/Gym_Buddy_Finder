package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.ChatPreviewDTO;
import com.vinit.gymPartner.dto.ChatMessageDTO;
import com.vinit.gymPartner.entity.ChatMessage;
import com.vinit.gymPartner.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.vinit.gymPartner.security.CustomUserDetails;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/previews")
    public ResponseEntity<List<ChatPreviewDTO>> getChatPreviews(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(chatService.getChatPreviews(userId));
    }

    @GetMapping("/{matchId}/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable Long matchId,
            Authentication authentication
    ){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(chatService.getChatHistory(matchId, userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(Map.of("count", chatService.getUnreadCount(userId)));
    }

    @GetMapping("/{matchId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCountForMatch(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(Map.of("count", chatService.getUnreadCountForMatch(userId, matchId)));
    }

    @PostMapping("/{matchId}/read")
    public ResponseEntity<Void> markMatchRead(@PathVariable Long matchId, Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        chatService.markMatchRead(matchId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendMessageRest(
            @RequestBody ChatMessageDTO messageDTO,
            Authentication authentication
    ){
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        log.info("REST chat message: authenticatedUserId={}, matchId={}",
                userId, messageDTO.getMatchId());

        ChatMessage saved = chatService.sendMessageAsUser(messageDTO, userId);

        // Broadcast via WebSocket for real-time delivery
        messagingTemplate.convertAndSend(
                "/topic/chat/" + messageDTO.getReceiverId(),
                saved
        );

        return ResponseEntity.ok(saved);
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO messageDTO){
        log.info("Received STOMP message: senderId={}, receiverId={}, matchId={}",
                messageDTO.getSenderId(), messageDTO.getReceiverId(), messageDTO.getMatchId());

        ChatMessage saved = chatService.sendMessage(messageDTO);

        // Send to a specific chat topic that the receiver subscribes to
        messagingTemplate.convertAndSend(
                "/topic/chat/" + messageDTO.getReceiverId(),
                saved
        );

        log.info("Message broadcast to /topic/chat/{}", messageDTO.getReceiverId());
    }

    @MessageExceptionHandler
    public void handleException(Exception e) {
        log.error("STOMP message handling error: {}", e.getMessage(), e);
    }
}
