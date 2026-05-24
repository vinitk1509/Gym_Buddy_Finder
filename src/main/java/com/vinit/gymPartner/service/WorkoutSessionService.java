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
import com.vinit.gymPartner.dto.AvailabilitySlotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutSessionService {

    private final WorkoutSessionRepository sessionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final EmailService emailService;
    private final UserService userService;


    public WorkoutSession createSession(Long matchId, Long creatorId, LocalDateTime start, LocalDateTime end)
    {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(()->new RuntimeException("Match not Found"));

        if(match.getStatus() != MatchStatus.ACCEPTED)
            throw new RuntimeException("Match not active");

        if(start.isBefore(LocalDateTime.now()))
            throw new RuntimeException("Session must be in future");

        if(!end.isAfter(start))
            throw new RuntimeException("Invalid session time");

        if(!match.getRequester().getId().equals(creatorId)
                && !match.getReceiver().getId().equals(creatorId))
            throw new RuntimeException("User not part of this match");

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateAvailability(match, start, end);
        validateOverlap(matchId, start, end);

        com.vinit.gymPartner.entity.Gym sessionGym = match.getGym();
        if (sessionGym == null) {
            sessionGym = match.getRequester().getGym();
        }

        WorkoutSession session = WorkoutSession.builder()
                .match(match)
                .gym(sessionGym)
                .startDateTime(start)
                .endDateTime(end)
                .createdBy(creator)
                .state(SessionState.PENDING_APPROVAL)  // Partner must approve first
                .requesterConfirmed(false)
                .receiverConfirmed(false)
                .requesterNoShow(false)
                .receiverNoShow(false)
                .createdAt(LocalDateTime.now())
                .build();

        WorkoutSession savedSession = sessionRepository.save(session);

        // Notify the partner to accept or decline
        User partner = match.getRequester().getId().equals(creatorId)
                ? match.getReceiver()
                : match.getRequester();

        String timeStr = start.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"));
        emailService.sendSessionProposalEmail(partner.getEmail(), partner.getName(), creator.getName(), timeStr);

        return savedSession;
    }
    public void approveSession(Long sessionId, Long userId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getState() != SessionState.PENDING_APPROVAL)
            throw new RuntimeException("Session is not waiting for approval");

        Match match = session.getMatch();

        // Only the partner (not the creator) can approve
        if (session.getCreatedBy().getId().equals(userId))
            throw new RuntimeException("You cannot approve your own session request");

        if (!match.getRequester().getId().equals(userId) && !match.getReceiver().getId().equals(userId))
            throw new RuntimeException("You are not part of this session");

        session.setState(SessionState.SCHEDULED);
        sessionRepository.save(session);

    }

    public void declineSession(Long sessionId, Long userId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getState() != SessionState.PENDING_APPROVAL)
            throw new RuntimeException("Session is not waiting for approval");

        Match match = session.getMatch();

        // Only the partner (not the creator) can decline
        if (session.getCreatedBy().getId().equals(userId))
            throw new RuntimeException("You cannot decline your own session request");

        if (!match.getRequester().getId().equals(userId) && !match.getReceiver().getId().equals(userId))
            throw new RuntimeException("You are not part of this session");

        session.setState(SessionState.DECLINED);
        sessionRepository.save(session);

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

            userService.updateReliability(match.getRequester(), +5);
            userService.updateReliability(match.getReceiver(), +5);
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

            userService.updateReliability(match.getReceiver(), -20);

        }

        else if(match.getReceiver().getId().equals(reporterId)){
            session.setRequesterNoShow(true);

            userService.updateReliability(match.getRequester(), -20);

        }
    }

    public void cancelSession(Long sessionId, Long userId){

        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Match match = session.getMatch();

        if(!match.getRequester().getId().equals(userId)
                && !match.getReceiver().getId().equals(userId))
            throw new RuntimeException("Not allowed");

        if (session.getState() != SessionState.PENDING_APPROVAL
                && session.getState() != SessionState.SCHEDULED) {
            throw new RuntimeException("Only pending or scheduled sessions can be cancelled");
        }

        if (session.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Past sessions cannot be cancelled");
        }

        int reliabilityDelta = session.getState() == SessionState.SCHEDULED
                && Duration.between(LocalDateTime.now(), session.getStartDateTime()).toHours() < 12
                ? UserService.LATE_CANCEL_PENALTY
                : -3;

        session.setState(SessionState.CANCELLED);

        User canceller = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userService.updateReliability(
                userId.equals(match.getRequester().getId())
                        ? match.getRequester()
                        : match.getReceiver(),
                reliabilityDelta
        );

    }

    public List<WorkoutSession> getSessionsByUser(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

    public List<AvailabilitySlotDTO> getMutualAvailability(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return buildMutualAvailability(match);
    }

    public List<AvailabilitySlotDTO> getMutualAvailability(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getRequester().getId().equals(userId)
                && !match.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("You are not part of this match");
        }

        return buildMutualAvailability(match);
    }

    private List<AvailabilitySlotDTO> buildMutualAvailability(Match match) {
        List<AvailabilitySlot> requesterSlots = availabilitySlotRepository.findByUserId(match.getRequester().getId());
        List<AvailabilitySlot> receiverSlots = availabilitySlotRepository.findByUserId(match.getReceiver().getId());

        List<AvailabilitySlotDTO> mutualSlots = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (AvailabilitySlot r : requesterSlots) {
            for (AvailabilitySlot s : receiverSlots) {
                if (r.getDayOfWeek().equals(s.getDayOfWeek())) {
                    // Check for overlap: r.start < s.end AND r.end > s.start
                    if (r.getStartTime().isBefore(s.getEndTime()) && r.getEndTime().isAfter(s.getStartTime())) {
                        
                        LocalTime mutualStart = r.getStartTime().isAfter(s.getStartTime()) ? r.getStartTime() : s.getStartTime();
                        LocalTime mutualEnd = r.getEndTime().isBefore(s.getEndTime()) ? r.getEndTime() : s.getEndTime();

                        String key = r.getDayOfWeek().name() + "-" + mutualStart + "-" + mutualEnd;
                        if (seen.add(key)) {
                            mutualSlots.add(AvailabilitySlotDTO.builder()
                                    .dayOfWeek(r.getDayOfWeek().name())
                                    .startTime(mutualStart.toString())
                                    .endTime(mutualEnd.toString())
                                    .build());
                        }
                    }
                }
            }
        }

        return mutualSlots;
    }
}
