package com.vinit.gymPartner.dto;

import com.vinit.gymPartner.entity.enums.ReportReason;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDTO {

    private ReportReason reason;
    private String description;
}