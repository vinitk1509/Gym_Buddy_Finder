package com.vinit.gymPartner.entity;

import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false)
    private Integer reliabilityScore;

    // FIXED: Added 'age' and 'gender' fields to resolve "cannot find symbol" compilation errors
    // DashboardService and MatchingService were relying on getAge() and getGender() which didn't exist.
    private Integer age;

    // Date of birth — used to auto-calculate age
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    
    private Gender gender;

    @Column(length = 500)
    private String profilePictureUrl;

    @Column(length = 300)
    private String bio;

    @Column(length = 15)
    private String phoneNumber;

    @Column(nullable = false)
    private Boolean allowMultiplePartners;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    // FIXED: Added @Builder.Default so Lombok respects the default value 'true' during object building
    @Builder.Default
    @Column(nullable = false)
    private boolean lookingForPartner = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FitnessProfile fitnessProfile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
