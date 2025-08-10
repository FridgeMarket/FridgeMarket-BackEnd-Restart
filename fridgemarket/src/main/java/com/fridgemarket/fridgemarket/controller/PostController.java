package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.DAO.Post;
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter;
import com.fridgemarket.fridgemarket.service.PostService;
import com.fridgemarket.fridgemarket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
/*
 * 제공 API:
 * - POST /api/posts/add-post: 게시글 작성
 * - GET /api/posts/check-post/{id}: 게시글 조회
 * - PUT /api/posts/update-post/{id}: 게시글 수정
 * - DELETE /api/posts/delete-post/{id}: 게시글 삭제
 * - GET /api/posts/search-post: 게시글 검색
 * - POST /api/posts/upload-image: 이미지 업로드
 */
@RestController
public class PostController {

    // 게시글 서비스 (의존성 주입)
    @Autowired
    private PostService postService;

    // 사용자 서비스 (의존성 주입)
    @Autowired
    private UserService userService;

    // 파일 업로드 경로 설정 (application.yml에서 주입)
    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // JWT 토큰으로만 인증 허용
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails) {
            JwtAuthenticationFilter.JwtUserDetails jwtDetails = (JwtAuthenticationFilter.JwtUserDetails) authentication.getDetails();
            return userService.findByProviderAndSocialId(jwtDetails.getProvider(), jwtDetails.getSocialId()).orElse(null);
        }
        
        // OAuth2 인증은 허용하지 않음 (JWT 토큰 필요)
        return null;
    }

    @PostMapping("/add-post")
    public ResponseEntity<Post> addPost(@RequestBody Post post) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        post.setUser(currentUser);
        Post newPost = postService.addPost(post);
        return new ResponseEntity<>(newPost, HttpStatus.CREATED);
    }

    @GetMapping("/check-post/{id}")
    public ResponseEntity<Post> getPost(@PathVariable Long id) {
        Optional<Post> post = postService.getPost(id);
        return post.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/update-post/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @RequestBody Post postDetails) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        if (id == null || id <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            Post updatedPost = postService.updatePost(id, postDetails, currentUser);
            return updatedPost != null ? new ResponseEntity<>(updatedPost, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete-post/{id}")
    public ResponseEntity<HttpStatus> deletePost(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        if (id == null || id <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            postService.deletePost(id, currentUser);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (SecurityException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    //게시물 검색 기능
    @GetMapping("/search-post")
    public ResponseEntity<List<Post>> searchPosts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        
        List<Post> posts;
        
        if (category != null && !category.isEmpty() && status != null && !status.isEmpty()) {
            Boolean statusBoolean = Boolean.parseBoolean(status);
            posts = postService.getPostsByCategoryAndStatus(category, statusBoolean);
        }
        else if (category != null && !category.isEmpty()) {
            posts = postService.getPostsByCategory(category);
        }
        else if (status != null && !status.isEmpty()) {
            Boolean statusBoolean = Boolean.parseBoolean(status);
            posts = postService.getPostsByStatus(statusBoolean);
        }
        else if (query != null && !query.isEmpty()) {
            posts = postService.searchPosts(query);
        }
        else {
            posts = postService.getAllPosts();
        }
        
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    // 카테고리별 검색
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Post>> getPostsByCategory(@PathVariable String category) {
        List<Post> posts = postService.getPostsByCategory(category);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    // 상태별 검색
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Post>> getPostsByStatus(@PathVariable Boolean status) {
        List<Post> posts = postService.getPostsByStatus(status);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    // 카테고리와 상태로 검색
    @GetMapping("/category/{category}/status/{status}")
    public ResponseEntity<List<Post>> getPostsByCategoryAndStatus(
            @PathVariable String category, 
            @PathVariable Boolean status) {
        List<Post> posts = postService.getPostsByCategoryAndStatus(category, status);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    // 이미지 업로드 엔드포인트
    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isValidImageFile(originalFilename)) {
                return ResponseEntity.badRequest().body("Invalid image file");
            }

            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            String imageUrl = "/uploads/" + uniqueFilename;
            return ResponseEntity.ok(imageUrl);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload image");
        }
    }

    private boolean isValidImageFile(String filename) {
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        String lowerFilename = filename.toLowerCase();
        
        for (String ext : allowedExtensions) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
}
