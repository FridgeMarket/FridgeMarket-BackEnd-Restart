package com.fridgemarket.fridgemarket.DAO;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Fridge {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fridge_num")
    private Long fridgenum;

    @ManyToOne
    @JoinColumn(name = "user_num")
    private User user;

    @Column(name = "food_name")
    private String foodname;

    @Column(name = "tag")
    private String tag;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "is_opened")
    private Boolean isopened;

    @Column(name = "expiration_date_before_open")
    private Date beforeopen;

    @Column(name = "expiration_date_after_open")
    private Date afteropen;

    @Column(name = "registered_at")
    private Date registeredat;

}