package com.vinit.gymPartner.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateGroupSessionRequestDTO {
    private Long groupId;
    private LocalDateTime start;
    private LocalDateTime end;
}
