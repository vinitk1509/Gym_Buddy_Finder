package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.entity.Gym;
import com.vinit.gymPartner.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gyms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Just in case, to match other controllers
public class GymController {

    private final GymRepository gymRepository;

    @GetMapping("/search")
    public ResponseEntity<List<Gym>> searchGyms(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        
        String trimmedQuery = query.trim();
        List<Gym> gyms = gymRepository
                .findTop10ByNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrCityContainingIgnoreCaseOrCountryContainingIgnoreCase(
                        trimmedQuery,
                        trimmedQuery,
                        trimmedQuery,
                        trimmedQuery
                );
        return ResponseEntity.ok(gyms);
    }
}
