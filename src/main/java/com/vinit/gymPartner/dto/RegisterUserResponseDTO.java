package com.vinit.gymPartner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterUserResponseDTO {

    private Long userId;
    private String fullName;
    private String email;
    private String message;
}
