package com.vinit.gymPartner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedUserDTO {
    private Long userId;
    private String name;
    private String email;
    private String profilePictureUrl;
    private LocalDateTime blockedAt;
}
