package com.fridgemarket.fridgemarket.repository;

import com.fridgemarket.fridgemarket.DAO.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByTitleContainingOrContentContaining(String title, String content);
    
    // 카테고리별 검색
    List<Post> findByTag(String tag);
    
    // 상태별 검색
    List<Post> findByStatus(Boolean status);
    
    // 카테고리와 상태로 검색
    List<Post> findByTagAndStatus(String tag, Boolean status);
}
