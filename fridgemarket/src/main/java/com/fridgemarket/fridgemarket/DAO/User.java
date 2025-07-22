package com.fridgemarket.fridgemarket.DAO;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "User")
public class User {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_num")
    private Long usernum;

    @Column(name = "user_id")
    private String userid;

    @Column(name = "name")
    private String name;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "agreed")
    private Boolean agreed;

    @Column(name = "provider")
    private String provider;

    @Column(name = "profile_num")
    private Integer profilenum;

    @Column(name = "refresh_token")
    private String refreshToken;
}
