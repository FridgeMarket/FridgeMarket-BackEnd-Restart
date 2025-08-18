package com.fridgemarket.fridgemarket.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class UserUpdateDto {
    private String socialId;
    private String provider;
    private String nickname;
    private String phone;
    private String address;
    private boolean agreed;
    private LocalDate birth;
    private String profileurl;
}