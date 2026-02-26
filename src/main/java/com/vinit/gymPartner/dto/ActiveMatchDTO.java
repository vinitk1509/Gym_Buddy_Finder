package com.vinit.gymPartner.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveMatchDTO {

    private Long matchId;
    private Long partnerId;
    private String partnerName;
    private int partnerAge;
    private String goal;
    private String experience;
}
