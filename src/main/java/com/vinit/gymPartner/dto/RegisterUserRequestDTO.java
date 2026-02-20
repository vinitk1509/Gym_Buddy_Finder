package com.vinit.gymPartner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterUserRequestDTO {

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotNull
    private String goal;

    @NotNull
    private String workoutType;

    @NotNull
    private String experienceLevel;

    @NotBlank
    private String gymName;

    @NotBlank
    private String gymAddress;


}
