package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.ChatMessageDTO;
import com.vinit.gymPartner.entity.ChatMessage;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.repository.ChatMessageRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final MatchRepository matchRepository;

    public ChatMessage sendMessage(ChatMessageDTO dto){
        var match = matchRepository.findById(dto.getMatchId())
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

        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getChatHistory(Long matchId){
        return chatMessageRepository.findByMatchIdOrderBySentAtAsc(matchId);
    }
}
