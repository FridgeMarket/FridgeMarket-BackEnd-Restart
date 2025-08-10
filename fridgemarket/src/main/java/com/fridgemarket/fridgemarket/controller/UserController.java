package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import com.fridgemarket.fridgemarket.service.UserService;
import com.fridgemarket.fridgemarket.dto.TokenResponse;
import com.fridgemarket.fridgemarket.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    // 사용자 정보 저장소 (생성자 주입)
    private final AppUserRepository appUserRepository;
    // 사용자 서비스 (생성자 주입)
    private final UserService userService;
    // JWT 토큰 유틸리티 (생성자 주입)
    private final JwtUtil jwtUtil;

    @GetMapping("/login")
    public String loginPage() {
        return "login";  // src/main/resources/templates/login.html
    }

   //디버깅용 api
    @GetMapping("/api/debug-auth")
    @ResponseBody
    public ResponseEntity<String> debugAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok("인증됨: " + authentication.getName() + 
                                   ", 타입: " + authentication.getClass().getSimpleName());
        } else {
            return ResponseEntity.ok("인증되지 않음");
        }
    }

    //OAuth2 디버깅 api
    @GetMapping("/api/debug-oauth2")
    @ResponseBody
    public ResponseEntity<String> debugOAuth2(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
                org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Token = 
                    (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication;
                
                org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                    (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                
                return ResponseEntity.ok("OAuth2 인증됨 - 제공자: " + oauth2Token.getAuthorizedClientRegistrationId() + 
                                       ", 사용자ID: " + oauth2User.getName());
            } else {
                return ResponseEntity.ok("OAuth2가 아닌 인증: " + authentication.getClass().getSimpleName());
            }
        } else {
            return ResponseEntity.ok("인증되지 않음");
        }
    }

    //로그인 후 메인
    @GetMapping("/success")
    public String successPage(Authentication authentication, Model model) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        String socialId = token.getPrincipal().getName();

        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        if (userOptional.isEmpty()) {
            // 신규 사용자라면 user-info로 리다이렉트
            return "redirect:/user-info?socialId=" + socialId + "&provider=" + provider;
        }
        User user = userOptional.get();
        model.addAttribute("user", user);
        return "success";  // src/main/resources/templates/success.html
    }

    // 통합된 사용자 정보 관리 (신규/기존 모두 처리)
    @GetMapping("/user-info")
    public String userInfoForm(@RequestParam(required = false) String socialId,
                              @RequestParam(required = false) String provider,
                              Authentication authentication,
                              Model model) {
        
        // 파라미터가 없는 경우 인증된 사용자 정보에서 가져오기
        if (socialId == null || provider == null) {
            if (authentication != null && authentication.isAuthenticated()) {
                // OAuth2 인증 처리
                if (authentication instanceof OAuth2AuthenticationToken) {
                    OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
                    provider = token.getAuthorizedClientRegistrationId();
                    socialId = token.getPrincipal().getName();
                }
                // JWT 토큰 인증 처리
                else if (authentication instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken token = 
                        (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) authentication;
                    
                    if (token.getDetails() instanceof com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter.JwtUserDetails) {
                        com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter.JwtUserDetails jwtDetails = 
                            (com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter.JwtUserDetails) token.getDetails();
                        provider = jwtDetails.getProvider();
                        socialId = jwtDetails.getSocialId();
                    }
                }
                
                // provider와 socialId가 여전히 null인 경우
                if (provider == null || socialId == null) {
                    return "redirect:/login";
                }
            } else {
                // 인증되지 않은 경우 로그인 페이지로 리다이렉트
                return "redirect:/login";
            }
        }
        
        // 기존 사용자 정보 조회
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        if (userOptional.isPresent()) {
            // 기존 사용자 - 정보 수정 모드
            model.addAttribute("user", userOptional.get());
            model.addAttribute("isNewUser", false);
        } else {
            // 신규 사용자 - 정보 입력 모드
            User newUser = new User();
            newUser.setUserid(socialId);
            newUser.setProvider(provider);
            newUser.setAdmin(false);
            model.addAttribute("user", newUser);
            model.addAttribute("isNewUser", true);
        }
        
        return "userinfo";  // 통합된 템플릿 사용
    }
    
    // 통합된 사용자 정보 저장 (신규/기존 모두 처리)
    @PostMapping("/user-info")
    public String saveUserInfo(@ModelAttribute User user, @RequestParam("profileImage") MultipartFile profileImage) {
        userService.updateUser(user, profileImage);
        return "redirect:/success";
    }


    //사용자 정보 JSON반환
    @GetMapping("/api/current-user")
    @ResponseBody
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            return ResponseEntity.status(401).build();
        }
        
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        String socialId = token.getPrincipal().getName();
        
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.status(404).build();
        }
    }

    //게시판
    @GetMapping("/posts")
    public String postCrudPage() {
        return "post-crud"; // src/main/resources/templates/post-crud.html
    }

    //토큰 만들고 json반환
    @GetMapping("/api/token")
    @ResponseBody
    public ResponseEntity<TokenResponse> getToken(Authentication authentication) {
        System.out.println("=== getToken 메서드 호출 ===");
        System.out.println("Authentication: " + (authentication != null ? authentication.getClass().getSimpleName() : "null"));
        System.out.println("Authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null"));
        
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("인증되지 않음");
            return ResponseEntity.status(401).build();
        }
        
        String provider = null;
        String socialId = null;
        
        // JWT 토큰으로만 인증 허용
        if (authentication instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken token = 
                (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) authentication;
            
            if (token.getDetails() instanceof com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter.JwtUserDetails) {
                com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter.JwtUserDetails jwtDetails = 
                    (com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter.JwtUserDetails) token.getDetails();
                provider = jwtDetails.getProvider();
                socialId = jwtDetails.getSocialId();
                System.out.println("JWT 인증 - 제공자: " + provider + ", 사용자ID: " + socialId);
            }
        }
        
        // OAuth2 인증은 허용하지 않음 (JWT 토큰 필요)
        if (provider == null || socialId == null) {
            System.out.println("JWT 토큰이 필요합니다");
            return ResponseEntity.status(401).build();
        }
        
        if (provider == null || socialId == null) {
            System.out.println("사용자 정보 추출 실패");
            return ResponseEntity.status(401).build();
        }
        
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            System.out.println("사용자 찾음: " + user.getNickname());
            
            // 기존 RefreshToken 사용 (새로 생성하지 않음)
            String refreshToken = user.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                // RefreshToken이 없는 경우에만 새로 생성
                refreshToken = jwtUtil.generateRefreshToken(user.getUsernum());
                user.setRefreshToken(refreshToken);
                appUserRepository.save(user);
                System.out.println("새로운 RefreshToken 생성");
            } else {
                System.out.println("기존 RefreshToken 사용");
            }
            
            // AccessToken만 새로 생성
            String accessToken = jwtUtil.generateAccessToken(user.getUsernum(), provider, socialId);
            
            // TokenResponse 생성
            TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
                user.getUsernum(),
                user.getNickname(),
                user.getEmail(),
                user.getProvider(),
                user.getProfileurl()
            );
            
            TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                refreshToken,
                1800, // 30분 (초 단위)
                604800, // 7일 (초 단위)
                userInfo
            );
            
            System.out.println("JWT 토큰 생성 완료");
            return ResponseEntity.ok(tokenResponse);
        } else {
            System.out.println("사용자를 찾을 수 없음");
            return ResponseEntity.status(404).build();
        }
    }

    // 토큰 재발급 at, rt
    @PostMapping("/api/token/refresh")
    @ResponseBody
    public ResponseEntity<TokenResponse> refreshToken(@RequestHeader("Authorization") String refreshTokenHeader) {
        if (refreshTokenHeader == null || !refreshTokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        
        String refreshToken = refreshTokenHeader.substring(7);
        
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            Long userNum = jwtUtil.getUserNumFromToken(refreshToken);
            Optional<User> userOptional = appUserRepository.findById(userNum);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                
                // DB에 저장된 RefreshToken과 비교
                if (!refreshToken.equals(user.getRefreshToken())) {
                    return ResponseEntity.status(401).build();
                }
                
                // 새로운 토큰 생성
                String newAccessToken = jwtUtil.generateAccessToken(user.getUsernum(), user.getProvider(), user.getUserid());
                String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsernum());
                
                // 새로운 RefreshToken을 DB에 저장
                user.setRefreshToken(newRefreshToken);
                appUserRepository.save(user);
                
                // TokenResponse 생성
                TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
                    user.getUsernum(),
                    user.getNickname(),
                    user.getEmail(),
                    user.getProvider(),
                    user.getProfileurl()
                );
                
                TokenResponse tokenResponse = new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    1800, // 30분 (초 단위)
                    604800, // 7일 (초 단위)
                    userInfo
                );
                
                return ResponseEntity.ok(tokenResponse);
            } else {
                return ResponseEntity.status(404).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
