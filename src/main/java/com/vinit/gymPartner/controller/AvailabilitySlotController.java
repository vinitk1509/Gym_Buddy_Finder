package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.AvailabilitySlotDTO;
import com.vinit.gymPartner.entity.AvailabilitySlot;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.repository.AvailabilitySlotRepository;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/availability")
@RequiredArgsConstructor
public class AvailabilitySlotController {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AvailabilitySlotDTO>> getMySlots(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AvailabilitySlot> slots = availabilitySlotRepository.findByUserId(user.getId());

        List<AvailabilitySlotDTO> dtos = slots.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<List<AvailabilitySlotDTO>> saveSlots(
            Authentication authentication,
            @RequestBody List<AvailabilitySlotDTO> slotDTOs) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete existing slots
        List<AvailabilitySlot> existingSlots = availabilitySlotRepository.findByUserId(user.getId());
        availabilitySlotRepository.deleteAll(existingSlots);

        // Save new slots
        List<AvailabilitySlot> newSlots = slotDTOs.stream().map(dto -> {
            AvailabilitySlot slot = new AvailabilitySlot();
            slot.setUser(user);
            slot.setDayOfWeek(parseDayOfWeek(dto.getDayOfWeek()));
            slot.setStartTime(LocalTime.parse(dto.getStartTime()));
            slot.setEndTime(LocalTime.parse(dto.getEndTime()));
            return slot;
        }).collect(Collectors.toList());

        availabilitySlotRepository.saveAll(newSlots);

        List<AvailabilitySlotDTO> savedDtos = newSlots.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(savedDtos);
    }

    private DayOfWeek parseDayOfWeek(String dayStr) {
        return switch (dayStr.toUpperCase().substring(0, 3)) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> DayOfWeek.valueOf(dayStr.toUpperCase());
        };
    }

    private String formatDayOfWeek(DayOfWeek day) {
        return day.name().substring(0, 1) + day.name().substring(1).toLowerCase();
    }

    private AvailabilitySlotDTO convertToDTO(AvailabilitySlot slot) {
        return AvailabilitySlotDTO.builder()
                .id(slot.getId())
                .dayOfWeek(formatDayOfWeek(slot.getDayOfWeek()).substring(0, 3)) // e.g. "Mon"
                .startTime(slot.getStartTime().toString())
                .endTime(slot.getEndTime().toString())
                .build();
    }
}
