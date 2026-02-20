package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.RegisterUserRequestDTO;
import com.vinit.gymPartner.dto.RegisterUserResponseDTO;
import com.vinit.gymPartner.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponseDTO> register(
            @Valid @RequestBody RegisterUserRequestDTO request
            ){
        RegisterUserResponseDTO response = userService.registerUser(request);
        return ResponseEntity.ok(response);
    }
}
