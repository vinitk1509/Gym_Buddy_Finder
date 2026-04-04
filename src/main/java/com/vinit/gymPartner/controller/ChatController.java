package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.ChatMessageDTO;
import com.vinit.gymPartner.entity.ChatMessage;
import com.vinit.gymPartner.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{matchId}/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long matchId){
        return ResponseEntity.ok(chatService.getChatHistory(matchId));
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO messageDTO){
        ChatMessage saved = chatService.sendMessage(messageDTO);
        // Send to a specific chat topic that the receiver subscribes to
        messagingTemplate.convertAndSend(
                "/topic/chat/" + messageDTO.getReceiverId(),
                saved
        );
    }
}
