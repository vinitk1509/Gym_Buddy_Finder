package com.vinit.gymPartner.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class profileUpdateDTO {
    private String name;
    private String bio;
    private Integer age;
    private String phoneNumber;
}
