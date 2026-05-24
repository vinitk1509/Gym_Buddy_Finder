package com.vinit.gymPartner.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponseDTO {

    private boolean matchingEnabled;
    private boolean profileComplete;
    private boolean hasActiveMatch;

    private int blockedCount;

    private List<MatchResultDTO> suggestedPartners;
    private List<MatchResponseDTO> activeMatches;
    private List<MatchResponseDTO> pendingSentRequests;
    private List<MatchResponseDTO> pendingReceivedRequests;
    private String message;
}
