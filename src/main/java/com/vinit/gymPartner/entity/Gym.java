package com.vinit.gymPartner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gyms",
uniqueConstraints = @UniqueConstraint(
        columnNames = {"name", "address"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String country;
    private String city;
    private String address;

    // Nullable: coordinates are set later by frontend map picker
    private Double latitude;

    // Nullable: coordinates are set later by frontend map picker
    private Double longitude;

    @Column(nullable = false)
    private String timezone;

    private LocalDateTime createdAt;

}
