package com.vinit.gymPartner.entity;

import com.vinit.gymPartner.entity.enums.ExperienceLevel;
import com.vinit.gymPartner.entity.enums.FitnessGoal;
import com.vinit.gymPartner.entity.enums.WorkoutType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fitness_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FitnessGoal goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkoutType workoutType;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}
