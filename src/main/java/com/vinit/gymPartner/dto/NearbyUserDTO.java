package com.vinit.gymPartner.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NearbyUserDTO {
    private Long userId;
    private String name;
    private String profilePicture;
    private String gymName;
    private double distanceKm;
    private double compatibilityScore;
}
