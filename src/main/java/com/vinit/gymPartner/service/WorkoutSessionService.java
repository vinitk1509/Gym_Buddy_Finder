package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.WorkoutSession;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.entity.enums.SessionState;
import com.vinit.gymPartner.repository.AvailabilitySlotRepository;
import com.vinit.gymPartner.repository.MatchRepository;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutSessionService {

    private final WorkoutSessionRepository sessionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;


    public WorkoutSession createSession(Long matchId, Long creatorId, LocalDateTime start, LocalDateTime end)
    {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(()->new RuntimeException("Match not Found"));

        if(match.getStatus() != MatchStatus.ACCEPTED)
            throw new RuntimeException("Match not active");

        if(start.isBefore(LocalDateTime.now()))
            throw new RuntimeException("Session must be in future");

        if(end.isBefore(start))
            throw new RuntimeException("Invalid session time");

        if(!match.getRequester().getId().equals(creatorId)
                && !match.getReceiver().getId().equals(creatorId))
            throw new RuntimeException("User not part of this match");

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateAvailability(match, start, end);
        validateOverlap(matchId, start, end);

        WorkoutSession session = WorkoutSession.builder()
                .match(match)
                .gym(match.getGym())
                .startDateTime(start)
                .endDateTime(end)
                .createdBy(creator)
                .state(SessionState.SCHEDULED)
                .requesterConfirmed(false)
                .receiverConfirmed(false)
                .requesterNoShow(false)
                .receiverNoShow(false)
                .createdAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(session);
    }
    private void validateAvailability(
            Match match,
            LocalDateTime start,
            LocalDateTime end) {

        List<AvailabilitySlot> requesterSlots =
                availabilitySlotRepository.findByUserId(match.getRequester().getId());

        List<AvailabilitySlot> receiverSlots =
                availabilitySlotRepository.findByUserId(match.getReceiver().getId());

        boolean valid = requesterSlots.stream().anyMatch(r ->
                receiverSlots.stream().anyMatch(s ->
                        r.getDayOfWeek().equals(start.getDayOfWeek()) &&
                                s.getDayOfWeek().equals(start.getDayOfWeek()) &&

                                !r.getStartTime().isAfter(start.toLocalTime()) &&
                                !r.getEndTime().isBefore(end.toLocalTime()) &&

                                !s.getStartTime().isAfter(start.toLocalTime()) &&
                                !s.getEndTime().isBefore(end.toLocalTime())
                )
        );

        if(!valid)
            throw new RuntimeException("Session must be inside overlapping availability");
    }

    private void validateOverlap(
            Long matchId,
            LocalDateTime start,
            LocalDateTime end){

        List<WorkoutSession> sessions =
                sessionRepository.findOverlappingSessions(matchId, start, end);

        if(!sessions.isEmpty())
            throw new RuntimeException("Session already scheduled in this time");
    }
    public void confirmAttendance(Long sessionId, Long userId){

        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Match match = session.getMatch();

        if(match.getRequester().getId().equals(userId))
            session.setRequesterConfirmed(true);

        else if(match.getReceiver().getId().equals(userId))
            session.setReceiverConfirmed(true);

        else
            throw new RuntimeException("User not part of this session");

        if(Boolean.TRUE.equals(session.getRequesterConfirmed())
                && Boolean.TRUE.equals(session.getReceiverConfirmed())){

            session.setState(SessionState.COMPLETED);

            UserService.updateReliability(match.getRequester(), +5);
            UserService.updateReliability(match.getReceiver(), +5);
        }
    }

    public void reportNoShow(Long sessionId, Long reporterId){

        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Match match = session.getMatch();

        if(match.getRequester().getId().equals(reporterId)){
            session.setReceiverNoShow(true);
        }

        else if(match.getReceiver().getId().equals(reporterId)){
            session.setRequesterNoShow(true);
        }

        else{
            throw new RuntimeException("User not part of this session");
        }

        session.setState(SessionState.NO_SHOW);
        if(match.getRequester().getId().equals(reporterId)){
            session.setReceiverNoShow(true);

            UserService.updateReliability(match.getReceiver(), -20);
        }

        else if(match.getReceiver().getId().equals(reporterId)){
            session.setRequesterNoShow(true);

            UserService.updateReliability(match.getRequester(), -20);
        }
    }

    public void cancelSession(Long sessionId, Long userId){

        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Match match = session.getMatch();

        if(!match.getRequester().getId().equals(userId)
                && !match.getReceiver().getId().equals(userId))
            throw new RuntimeException("Not allowed");

        session.setState(SessionState.CANCELLED);
        session.setState(SessionState.CANCELLED);

        UserService.updateReliability(
                userId.equals(match.getRequester().getId())
                        ? match.getRequester()
                        : match.getReceiver(),
                -3
        );
    }
}
