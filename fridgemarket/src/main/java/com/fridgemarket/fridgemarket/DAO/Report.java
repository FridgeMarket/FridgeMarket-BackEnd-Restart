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
public class Report {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_num")
    private Long reportnum;

    @ManyToOne
    @JoinColumn(name = "user_num")
    private User user;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_time")
    private Date createdTime;

}