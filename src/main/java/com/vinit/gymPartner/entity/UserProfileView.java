package com.vinit.gymPartner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_profile_views",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"viewer_id", "viewed_user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;

    @ManyToOne
    @JoinColumn(name = "viewed_user_id", nullable = false)
    private User viewedUser;

    private LocalDateTime viewedAt;
}