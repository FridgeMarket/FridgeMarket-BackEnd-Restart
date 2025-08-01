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
public class Post {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_num")
    private Long postnum;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne
    @JoinColumn(name = "tag_num")
    private Tag tag;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "expiration_date")
    private Date expirationdate;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "created_at")
    private Date createdat;

    @Column(name = "refridger_food")
    private String refridgerfood;
}