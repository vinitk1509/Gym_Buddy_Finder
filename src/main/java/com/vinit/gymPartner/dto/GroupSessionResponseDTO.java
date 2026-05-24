package com.vinit.gymPartner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSessionResponseDTO {
    private Long id;
    private Long groupId;
    private String groupName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String state;
    private Long createdById;
    private String createdByName;
}
