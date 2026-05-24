package com.vinit.gymPartner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotDTO {
    private Long id;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
}
