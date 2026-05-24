package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymRepository extends JpaRepository<Gym, Long> {
    Optional<Gym> findByNameAndAddress(String name, String address);
    Optional<Gym> findByPlaceId(String placeId);
    java.util.List<Gym> findTop10ByNameContainingIgnoreCase(String name);

    List<Gym> findTop10ByNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrCityContainingIgnoreCaseOrCountryContainingIgnoreCase(
            String name,
            String address,
            String city,
            String country
    );
}
