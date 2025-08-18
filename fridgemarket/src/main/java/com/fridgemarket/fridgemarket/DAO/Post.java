package com.fridgemarket.fridgemarket.DAO;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Table(name = "Post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_num")
    private Long postnum;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "tag")
    private String tag;  // 음식 카테고리 (육류, 채소, 해산물, 과일, 유제품, 곡류, 가공식품)

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "expiration_date")
    private Date expirationdate;

    @Column(name = "status")
    private Boolean status;  // 나눔 상태 (true: 진행중, false: 완료)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Date createdat;

    @Column(name = "refridger_food")  // DB 컬럼명에 맞게 수정
    private String refridgerfood;
    
    @Column(name = "image_url")
    private String imageUrl;  // 이미지 URL 필드 추가
}