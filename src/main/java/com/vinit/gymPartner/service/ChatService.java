package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.ChatMessageDTO;
import com.vinit.gymPartner.entity.ChatMessage;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.enums.MatchStatus;
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
    private final ChatMessageRepository chatMessageRepository;
    private final MatchRepository matchRepository;
    private final NotificationService notificationService;

    @Transactional
    public ChatMessage sendMessage(ChatMessageDTO dto){
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

        ChatMessage message = ChatMessage.builder()
                .matchId(dto.getMatchId())
                .senderId(dto.getSenderId())
                .receiverId(dto.getReceiverId())
                .content(dto.getContent())
                .sentAt(LocalDateTime.now())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Eagerly resolve lazy-loaded names within the transaction
        String senderName = match.getRequester().getId().equals(dto.getSenderId())
                ? match.getRequester().getName()
                : match.getReceiver().getName();

        log.info("Chat message saved (id={}) from user {} to user {} in match {}",
                savedMessage.getId(), dto.getSenderId(), dto.getReceiverId(), dto.getMatchId());

        notificationService.sendToUser(
                dto.getReceiverId(),
                "New Message 💬",
                senderName + ": " + dto.getContent()
        );

        return savedMessage;
    }

    public List<ChatMessage> getChatHistory(Long matchId){
        return chatMessageRepository.findByMatchIdOrderBySentAtAsc(matchId);
    }
}
