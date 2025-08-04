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

    @Autowired
    private PostRepository postRepository;

    public Post addPost(Post post) {
        return postRepository.save(post);
    }

    public Optional<Post> getPost(Long id) {
        return postRepository.findById(id);
    }

    public Post updatePost(Long id, Post updatedPost, User currentUser) {
        Optional<Post> existingPostOptional = postRepository.findById(id);
        if (existingPostOptional.isPresent()) {
            Post existingPost = existingPostOptional.get();
            // Check if the current user is the owner of the post or an admin
            if (existingPost.getUser().getUsernum().equals(currentUser.getUsernum()) || (currentUser.getAdmin() != null && currentUser.getAdmin())) {
                existingPost.setTitle(updatedPost.getTitle());
                existingPost.setContent(updatedPost.getContent());
                existingPost.setTag(updatedPost.getTag());  // tag 필드 추가
                existingPost.setExpirationdate(updatedPost.getExpirationdate());
                existingPost.setStatus(updatedPost.getStatus());
                existingPost.setRefridgerfood(updatedPost.getRefridgerfood());
                existingPost.setImageUrl(updatedPost.getImageUrl());  // imageUrl 필드 추가
                return postRepository.save(existingPost);
            } else {
                throw new SecurityException("You are not authorized to update this post.");
            }
        } else {
            return null; // Or throw an exception indicating post not found
        }
    }

    public void deletePost(Long id, User currentUser) {
        Optional<Post> existingPostOptional = postRepository.findById(id);
        if (existingPostOptional.isPresent()) {
            Post existingPost = existingPostOptional.get();
            // Check if the current user is the owner of the post or an admin
            if (existingPost.getUser().getUsernum().equals(currentUser.getUsernum()) || (currentUser.getAdmin() != null && currentUser.getAdmin())) {
                postRepository.deleteById(id);
            } else {
                throw new SecurityException("You are not authorized to delete this post.");
            }
        } else {
            throw new IllegalArgumentException("Post not found with ID: " + id);
        }
    }

    public List<Post> searchPosts(String query) {
        return postRepository.findByTitleContainingOrContentContaining(query, query);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }
    
    // 카테고리별 검색
    public List<Post> getPostsByCategory(String category) {
        return postRepository.findByTag(category);
    }
    
    // 상태별 검색
    public List<Post> getPostsByStatus(Boolean status) {
        return postRepository.findByStatus(status);
    }
    
    // 카테고리와 상태로 검색
    public List<Post> getPostsByCategoryAndStatus(String category, Boolean status) {
        return postRepository.findByTagAndStatus(category, status);
    }
}
