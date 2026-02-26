package com.vinit.gymPartner.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingMatchDTO {

    private Long matchId;
    private Long userId;
    private String name;
    private String type; // SENT or RECEIVED
}
