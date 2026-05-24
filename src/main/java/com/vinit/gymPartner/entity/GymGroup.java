package com.vinit.gymPartner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "gym_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> members = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    private Integer targetCapacity;

    @Builder.Default
    private Boolean banned = false;

    private LocalDateTime bannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_id")
    private User bannedBy;

    private LocalDateTime createdAt;
}
