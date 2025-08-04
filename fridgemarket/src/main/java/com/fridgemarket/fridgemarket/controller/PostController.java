package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.DAO.Post;
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.service.PostService;
import com.fridgemarket.fridgemarket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    private User getCurrentUser(OAuth2User oauth2User) {
        if (oauth2User == null) {
            return null; // Or throw an exception for unauthenticated user
        }
        
        // OAuth2User에서 provider와 socialId 정보를 가져옴
        String provider = oauth2User.getAttribute("provider");
        String socialId = oauth2User.getAttribute("socialId");
        
        // provider와 socialId가 없는 경우 name을 socialId로 사용
        if (provider == null || socialId == null) {
            socialId = oauth2User.getName();
            // provider는 CustomOAuth2UserService에서 설정된 값을 사용
            provider = oauth2User.getAttribute("provider");
        }
        
        if (socialId == null) {
            return null;
        }
        
        return userService.findByProviderAndSocialId(provider, socialId).orElse(null);
    }

    @PostMapping("/add-post")
    public ResponseEntity<Post> addPost(@RequestBody Post post, @AuthenticationPrincipal OAuth2User oauth2User) {
        User currentUser = getCurrentUser(oauth2User);
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
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @RequestBody Post postDetails, @AuthenticationPrincipal OAuth2User oauth2User) {
        User currentUser = getCurrentUser(oauth2User);
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        // ID 검증
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
    public ResponseEntity<HttpStatus> deletePost(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oauth2User) {
        User currentUser = getCurrentUser(oauth2User);
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        // ID 검증
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

    @GetMapping("/search-post")
    public ResponseEntity<List<Post>> searchPosts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        
        List<Post> posts;
        
        // 카테고리와 상태가 모두 지정된 경우
        if (category != null && !category.isEmpty() && status != null && !status.isEmpty()) {
            Boolean statusBoolean = Boolean.parseBoolean(status);
            posts = postService.getPostsByCategoryAndStatus(category, statusBoolean);
        }
        // 카테고리만 지정된 경우
        else if (category != null && !category.isEmpty()) {
            posts = postService.getPostsByCategory(category);
        }
        // 상태만 지정된 경우
        else if (status != null && !status.isEmpty()) {
            Boolean statusBoolean = Boolean.parseBoolean(status);
            posts = postService.getPostsByStatus(statusBoolean);
        }
        // 검색어만 지정된 경우
        else if (query != null && !query.isEmpty()) {
            posts = postService.searchPosts(query);
        }
        // 아무것도 지정되지 않은 경우
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
            // 파일 확장자 검증
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isValidImageFile(originalFilename)) {
                return ResponseEntity.badRequest().body("Invalid image file");
            }

            // 업로드 디렉토리 생성
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 고유한 파일명 생성
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // 파일 저장
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            // URL 반환
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
