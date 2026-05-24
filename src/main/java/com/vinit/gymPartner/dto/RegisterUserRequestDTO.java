package com.vinit.gymPartner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

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

    @NotBlank
    private String emailVerificationCode;

    private String gender;

    @NotNull
    private LocalDate dateOfBirth;

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

    @Size(max = 500)
    private String bio;

    private Double latitude;
    private Double longitude;
    private String placeId;
}
