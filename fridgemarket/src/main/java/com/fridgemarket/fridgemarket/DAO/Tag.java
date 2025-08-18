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
public class Tag {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_num")
    private Long tagnum;

    @Column(name = "tag")
    private String tag;
}