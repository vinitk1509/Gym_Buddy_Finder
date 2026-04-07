package com.vinit.gymPartner.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "device_tokens")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    private String platform;
}
