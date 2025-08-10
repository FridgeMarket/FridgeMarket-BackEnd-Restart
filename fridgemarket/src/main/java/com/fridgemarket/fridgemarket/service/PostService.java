package com.fridgemarket.fridgemarket.service;

import com.fridgemarket.fridgemarket.DAO.Post;
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    // 게시글 저장소 (의존성 주입)
    @Autowired
    private PostRepository postRepository;

   //게시글 등록
    public Post addPost(Post post) {
        return postRepository.save(post);
    }

   //게시글 조회
    public Optional<Post> getPost(Long id) {
        return postRepository.findById(id);
    }

    //게시글 수정
    public Post updatePost(Long id, Post updatedPost, User currentUser) {
        Optional<Post> existingPostOptional = postRepository.findById(id);
        if (existingPostOptional.isPresent()) {
            Post existingPost = existingPostOptional.get();
            
            if (existingPost.getUser().getUsernum().equals(currentUser.getUsernum()) || 
                (currentUser.getAdmin() != null && currentUser.getAdmin())) {
                
                existingPost.setTitle(updatedPost.getTitle());
                existingPost.setContent(updatedPost.getContent());
                existingPost.setTag(updatedPost.getTag());
                existingPost.setExpirationdate(updatedPost.getExpirationdate());
                existingPost.setStatus(updatedPost.getStatus());
                existingPost.setRefridgerfood(updatedPost.getRefridgerfood());
                existingPost.setImageUrl(updatedPost.getImageUrl());
                
                return postRepository.save(existingPost);
            } else {
                throw new SecurityException("You are not authorized to update this post.");
            }
        } else {
            return null;
        }
    }

    //게시글 삭제
    public void deletePost(Long id, User currentUser) {
        Optional<Post> existingPostOptional = postRepository.findById(id);
        if (existingPostOptional.isPresent()) {
            Post existingPost = existingPostOptional.get();
            
            if (existingPost.getUser().getUsernum().equals(currentUser.getUsernum()) || 
                (currentUser.getAdmin() != null && currentUser.getAdmin())) {
                
                postRepository.deleteById(id);
            } else {
                throw new SecurityException("You are not authorized to delete this post.");
            }
        } else {
            throw new IllegalArgumentException("Post not found with ID: " + id);
        }
    }

    //제목 또는 내용으로 게시글 검색
    public List<Post> searchPosts(String query) {
        return postRepository.findByTitleContainingOrContentContaining(query, query);
    }

    //모든 게시글 조회
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }
    
    //게시글 카테고리로 조회
    public List<Post> getPostsByCategory(String category) {
        return postRepository.findByTag(category);
    }
    
    //상태로 조회
    public List<Post> getPostsByStatus(Boolean status) {
        return postRepository.findByStatus(status);
    }
    
    //카테고리, 상태로 조회
    public List<Post> getPostsByCategoryAndStatus(String category, Boolean status) {
        return postRepository.findByTagAndStatus(category, status);
    }
}
