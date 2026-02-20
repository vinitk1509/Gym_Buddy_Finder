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

    @Column(nullable = false)
    private String timezone;

    private LocalDateTime createdAt;

}
