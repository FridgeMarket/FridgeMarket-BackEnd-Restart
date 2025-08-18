package com.fridgemarket.fridgemarket.controller;

/**
 * 게시글 CRUD, 검색, 이미지 업로드를 처리하는 REST 컨트롤러.
 *
 * 인증 정책
 * - 작성/수정/삭제는 JWT 기반 인증만 허용(SecurityContext의 JwtUserDetails 필요)
 * - 조회/검색은 공개 혹은 보안 설정에 따름
 *
 * 제공 API
 * - POST /add-post                : 게시글 작성(인증 필요)
 * - GET  /check-post/{id}         : 게시글 단건 조회
 * - PUT  /update-post/{id}        : 게시글 수정(작성자 권한, 인증 필요)
 * - DELETE /delete-post/{id}      : 게시글 삭제(작성자 권한, 인증 필요)
 * - GET  /search-post             : 게시글 검색(쿼리/카테고리/상태 조합)
 * - GET  /all                     : 전체 목록 조회
 * - GET  /category/{category}     : 카테고리별 조회
 * - GET  /status/{status}         : 상태별 조회
 * - GET  /category/{c}/status/{s} : 카테고리+상태 조회
 * - POST /upload-image            : 이미지 업로드 → 공개 URL 반환
 */

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

    /**
     * 현재 요청의 JWT 인증 정보에서 사용자 객체를 조회하는 헬퍼
     * - SecurityContext의 Authentication.details가 JwtUserDetails 여야 함
     * - provider/socialId로 사용자 조회하여 반환, 미인증 시 null
     */
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

    /**
     * 게시글 작성
     * - 본문으로 전달된 Post에 현재 사용자 설정 후 저장
     * - 성공 시 201과 생성된 Post 반환, 미인증은 401
     */
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

    /**
     * 게시글 단건 조회
     * - 존재하면 200과 Post, 없으면 404
     */
    @GetMapping("/check-post/{id}")
    public ResponseEntity<Post> getPost(@PathVariable Long id) {
        Optional<Post> post = postService.getPost(id);
        return post.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * 게시글 수정
     * - path 변수 id와 본문 postDetails를 받아 서비스에서 작성자 권한 검증 후 수정
     * - 200(성공), 400(잘못된 요청), 401(미인증), 403(권한없음), 404(없음)
     */
    @PatchMapping("/update-post/{id}")
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

    /**
     * 게시글 삭제
     * - 작성자 본인만 삭제 가능
     * - 204(성공), 400/401/403/404 상황별 반환
     */
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
    /**
     * 게시글 검색
     * - query(키워드), category(카테고리), status(상태: true/false)를 조합하여 검색
     * - 파라미터 없으면 전체 조회
     */
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

    /**
     * 전체 게시글 조회
     */
    @GetMapping("/all")
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    /**
     * 카테고리별 게시글 조회
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Post>> getPostsByCategory(@PathVariable String category) {
        List<Post> posts = postService.getPostsByCategory(category);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    /**
     * 상태별 게시글 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Post>> getPostsByStatus(@PathVariable Boolean status) {
        List<Post> posts = postService.getPostsByStatus(status);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    /**
     * 카테고리와 상태를 동시에 조건으로 조회
     */
    @GetMapping("/category/{category}/status/{status}")
    public ResponseEntity<List<Post>> getPostsByCategoryAndStatus(
            @PathVariable String category, 
            @PathVariable Boolean status) {
        List<Post> posts = postService.getPostsByCategoryAndStatus(category, status);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    /**
     * 이미지 업로드
     * - 유효한 이미지 확장자만 허용
     * - 저장소에 UUID 파일명으로 저장 후 공개 URL(`/uploads/**`) 반환
     * - 저장 경로는 application.yml의 file.upload.path(기본값 uploads/)
     */
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
