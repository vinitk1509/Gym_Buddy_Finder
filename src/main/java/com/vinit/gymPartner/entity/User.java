package com.vinit.gymPartner.entity;

import com.vinit.gymPartner.entity.converter.GenderConverter;
import com.vinit.gymPartner.entity.enums.Gender;
import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
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

    private Integer age;

    private LocalDate dateOfBirth;

    @Convert(converter = GenderConverter.class)
    private Gender gender;

    private String profilePictureUrl;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer reliabilityScore = 100;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowMultiplePartners = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer targetGroupSize = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(nullable = false)
    @Builder.Default
    private boolean lookingForPartner = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FitnessProfile fitnessProfile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    private LocalDateTime lastSeenAt;

    private LocalDateTime deletionRequestedAt;

    @PrePersist
    @PreUpdate
    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

}
