package com.vinit.gymPartner.service;
import com.vinit.gymPartner.entity.enums.ExperienceLevel;
public class ExperienceScoringEngine {

    public static double score(ExperienceLevel e1, ExperienceLevel e2) {

        if (e1 == ExperienceLevel.BEGINNER && e2 == ExperienceLevel.BEGINNER)
            return 15.0;

        if (e1 == ExperienceLevel.INTERMEDIATE && e2 == ExperienceLevel.INTERMEDIATE)
            return 25.0;

        if (e1 == ExperienceLevel.ADVANCED && e2 == ExperienceLevel.ADVANCED)
            return 24.0;

        if ((e1 == ExperienceLevel.BEGINNER && e2 == ExperienceLevel.INTERMEDIATE) ||
                (e2 == ExperienceLevel.BEGINNER && e1 == ExperienceLevel.INTERMEDIATE))
            return 22.0;

        if ((e1 == ExperienceLevel.INTERMEDIATE && e2 == ExperienceLevel.ADVANCED) ||
                (e2 == ExperienceLevel.INTERMEDIATE && e1 == ExperienceLevel.ADVANCED))
            return 23.0;

        return 18.0;
    }
}