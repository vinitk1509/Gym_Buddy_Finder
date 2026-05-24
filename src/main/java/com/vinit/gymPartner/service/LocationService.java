package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.NearbyUserDTO;
import com.vinit.gymPartner.entity.Gym;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final UserRepository userRepository;

    // Earth's radius in kilometers (used for Haversine formula)
    private static final double EARTH_RADIUS_KM = 6371.0;

    public List<NearbyUserDTO> findNearbyUsers(User currentUser, double radiusKm) {

        Gym myGym = currentUser.getGym();

        // If current user's gym has no coordinates yet, can't calculate distances
        if (myGym == null || myGym.getLatitude() == null || myGym.getLongitude() == null) {
            return new ArrayList<>();
        }

        // Get all active users (we'll filter by distance)
        List<User> allActiveUsers = userRepository.findByStatus(UserStatus.ACTIVE);

        List<NearbyUserDTO> nearbyUsers = new ArrayList<>();

        for (User candidate : allActiveUsers) {
            // Skip self
            if (candidate.getId().equals(currentUser.getId())) continue;
            if (candidate.getRole() == UserRole.ADMIN) continue;
            if (!candidate.isLookingForPartner()) continue;

            Gym candidateGym = candidate.getGym();
            if (candidateGym == null) continue;

            // Skip gyms that don't have coordinates set yet
            if (candidateGym.getLatitude() == null || candidateGym.getLongitude() == null) continue;

            // Calculate distance between the two gyms
            double distance = haversine(
                    myGym.getLatitude(), myGym.getLongitude(),
                    candidateGym.getLatitude(), candidateGym.getLongitude()
            );

            // Only include users within the specified radius
            if (distance <= radiusKm) {
                nearbyUsers.add(NearbyUserDTO.builder()
                        .userId(candidate.getId())
                        .name(candidate.getName())
                        .profilePicture(candidate.getProfilePictureUrl())
                        .gymName(candidateGym.getName())
                        .distanceKm(Math.round(distance * 10.0) / 10.0) // Round to 1 decimal
                        .build()
                );
            }
        }

        // Sort by nearest first
        nearbyUsers.sort(Comparator.comparingDouble(NearbyUserDTO::getDistanceKm));
        return nearbyUsers;
    }


    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians (math functions need radians)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
