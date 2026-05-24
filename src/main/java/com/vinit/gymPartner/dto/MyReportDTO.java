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
public class MyReportDTO {
    private Long reportId;
    private Long reportedUserId;
    private String reportedUserName;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
