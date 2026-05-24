package com.vinit.gymPartner.dto;

import com.vinit.gymPartner.entity.enums.ExperienceLevel;
import com.vinit.gymPartner.entity.enums.FitnessGoal;
import com.vinit.gymPartner.entity.enums.Gender;
import com.vinit.gymPartner.entity.enums.WorkoutType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExploreFilterDTO {
    private FitnessGoal goal;
    private ExperienceLevel experience;
    private WorkoutType workoutType;
    private Gender gender;
    private Double minScore;

    private Integer minAge;
    private Integer maxAge;
    private Integer minWeeklyOverlapMinutes;
    private Double radiusKm;
}
