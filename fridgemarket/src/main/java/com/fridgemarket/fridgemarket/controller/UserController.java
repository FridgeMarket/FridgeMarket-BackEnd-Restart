package com.fridgemarket.fridgemarket.controller;

/**
 * 사용자 관련 화면 라우팅과 인증/토큰 관련 API를 제공하는 컨트롤러.
 *
 * 구성 요소
 * - 로그인/성공 화면 라우팅
 * - 사용자 정보 입력/수정 화면 및 저장
 * - 현재 사용자 정보 조회(API)
 * - JWT 발급 및 재발급(API)
 * - 인증 상태/타입 디버깅용 API
 *
 * 인증 정책 개요
 * - 화면 라우팅(`/login`, `/success`, `/user-info`)은 보안 설정에 따라 접근 가능
 * - `/api/token`, `/api/token/refresh`는 JWT 기반 인증/검증을 통해 동작
 * - OAuth2 인증 정보와 JWT 인증 정보를 모두 처리할 수 있도록 분기 처리되어 있음
 */

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import com.fridgemarket.fridgemarket.service.UserService;
import com.fridgemarket.fridgemarket.service.KakaoAuthService;
import com.fridgemarket.fridgemarket.dto.TokenResponse;
import com.fridgemarket.fridgemarket.dto.KakaoLoginRequest;
import com.fridgemarket.fridgemarket.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {

    // 사용자 정보 저장소 (생성자 주입)
    private final AppUserRepository appUserRepository;
    // 사용자 서비스 (생성자 주입)
    private final UserService userService;
    // 카카오 인증 서비스 (생성자 주입)
    private final KakaoAuthService kakaoAuthService;
    // JWT 토큰 유틸리티 (생성자 주입)
    private final JwtUtil jwtUtil;

    @GetMapping("/login")
    /**
     * 로그인 페이지 반환 (웹용)
     * - GET /login
     * - 뷰 템플릿: templates/login.html
     */
    public String loginPage() {
        return "login";  // src/main/resources/templates/login.html
    }

    @GetMapping("/api/login-status")
    @ResponseBody
    /**
     * 로그인 상태 확인 API (iOS용)
     * - GET /api/login-status
     * - 반환: 로그인 상태와 사용자 정보
     */
    public ResponseEntity<Object> getLoginStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("isLoggedIn", false));
        }
        
        // OAuth2 인증 처리
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            String provider = token.getAuthorizedClientRegistrationId();
            String socialId = token.getPrincipal().getName();
            
            Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                return ResponseEntity.ok(Map.of(
                    "isLoggedIn", true,
                    "user", Map.of(
                        "usernum", user.getUsernum(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail(),
                        "provider", user.getProvider(),
                        "profileurl", user.getProfileurl(),
                        "isRegistered", user.getIsRegistered()
                    )
                ));
            }
        }
        
        // JWT 인증 처리
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
            if (token.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails) {
                JwtAuthenticationFilter.JwtUserDetails jwtDetails = (JwtAuthenticationFilter.JwtUserDetails) token.getDetails();
                Optional<User> userOptional = appUserRepository.findByProviderAndUserid(jwtDetails.getProvider(), jwtDetails.getSocialId());
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    return ResponseEntity.ok(Map.of(
                        "isLoggedIn", true,
                        "user", Map.of(
                            "usernum", user.getUsernum(),
                            "nickname", user.getNickname(),
                            "email", user.getEmail(),
                            "provider", user.getProvider(),
                            "profileurl", user.getProfileurl(),
                            "isRegistered", user.getIsRegistered()
                        )
                    ));
                }
            }
        }
        
        return ResponseEntity.ok(Map.of("isLoggedIn", false));
    }

   //디버깅용 api
    @GetMapping("/api/debug-auth")
    @ResponseBody
    /**
     * 인증 객체의 존재/타입을 확인하기 위한 디버깅용 API
     * - GET /api/debug-auth
     * - 반환: 인증 여부와 Authentication 구현체 타입 문자열
     */
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
    /**
     * OAuth2 인증 정보를 확인하는 디버깅용 API
     * - GET /api/debug-oauth2
     * - OAuth2가 아닌 인증이면 해당 사실을 문자열로 반환
     */
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
    /**
     * 로그인 성공 후 메인 화면 라우팅 (웹용)
     * - GET /success
     * - OAuth2 인증 토큰에서 제공자/소셜ID 추출
     * - 신규 사용자는 사용자 정보 입력 화면으로 리다이렉트
     * - 기존 사용자는 사용자 정보를 모델에 담아 success 뷰 렌더링
     */
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

    @GetMapping("/api/user-profile")
    @ResponseBody
    /**
     * 사용자 프로필 정보 조회 API (iOS용)
     * - GET /api/user-profile
     * - OAuth2 또는 JWT 인증 모두 지원
     * - 신규 사용자는 isNewUser: true 반환
     * - 기존 사용자는 사용자 정보 반환
     */
    public ResponseEntity<Object> getUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String provider = null;
        String socialId = null;
        
        // OAuth2 인증 처리
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            provider = token.getAuthorizedClientRegistrationId();
            socialId = token.getPrincipal().getName();
        }
        // JWT 인증 처리
        else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
            if (token.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails) {
                JwtAuthenticationFilter.JwtUserDetails jwtDetails = (JwtAuthenticationFilter.JwtUserDetails) token.getDetails();
                provider = jwtDetails.getProvider();
                socialId = jwtDetails.getSocialId();
            }
        }
        
        if (provider == null || socialId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        if (userOptional.isPresent()) {
            // 기존 사용자
            User user = userOptional.get();
            return ResponseEntity.ok(Map.of(
                "isNewUser", false,
                "user", Map.of(
                    "usernum", user.getUsernum(),
                    "nickname", user.getNickname(),
                    "email", user.getEmail(),
                    "provider", user.getProvider(),
                    "profileurl", user.getProfileurl(),
                    "isRegistered", user.getIsRegistered()
                )
            ));
        } else {
            // 신규 사용자
            return ResponseEntity.ok(Map.of(
                "isNewUser", true,
                "user", Map.of(
                    "userid", socialId,
                    "provider", provider,
                    "admin", false
                )
            ));
        }
    }

    // 통합된 사용자 정보 관리 (신규/기존 모두 처리)
    @GetMapping("/user-info")
    /**
     * 사용자 정보 입력/수정 화면 라우팅(신규/기존 통합)
     * - GET /user-info
     * - 동작
     *   1) 요청 파라미터에 provider/socialId가 없으면 인증 객체에서 추출(OAuth2 또는 JWT 모두 지원)
     *   2) DB 조회 결과에 따라 신규/수정 모드로 `userinfo` 템플릿 렌더링
     *   3) 비인증 상태 또는 식별 불가 시 /login 으로 리다이렉트
     */
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
    /**
     * 사용자 정보 저장(신규/기존 통합)
     * - POST /user-info
     * - 폼 바인딩된 User와 프로필 이미지 파일을 서비스 계층으로 전달하여 저장
     * - 처리 후 /success로 리다이렉트
     */
    public String saveUserInfo(@ModelAttribute User user, @RequestParam("profileImage") MultipartFile profileImage) {
        userService.updateUser(user, profileImage);
        return "redirect:/success";
    }


    //사용자 정보 JSON반환
    @GetMapping("/api/current-user")
    @ResponseBody
    /**
     * 현재 로그인한 사용자 정보를 JSON으로 반환
     * - GET /api/current-user
     * - OAuth2 인증 전제(현재 구현상 JWT는 아님)
     * - 존재하지 않으면 404, 미인증은 401
     */
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
    /**
     * 게시판 기능 화면 라우팅 (웹용)
     * - GET /posts
     * - 뷰 템플릿: templates/post-crud.html
     */
    public String postCrudPage() {
        return "post-crud"; // src/main/resources/templates/post-crud.html
    }

    @GetMapping("/api/posts-page")
    @ResponseBody
    /**
     * 게시판 페이지 정보 API (iOS용)
     * - GET /api/posts-page
     * - 게시판 접근 가능 여부와 기본 정보 반환
     */
    public ResponseEntity<Object> getPostsPageInfo() {
        return ResponseEntity.ok(Map.of(
            "pageName", "게시판",
            "description", "냉장고 재료 거래 게시판",
            "available", true
        ));
    }

    // 쪽지 테스트 페이지 라우팅
    @GetMapping("/chat-test")
    /**
     * 쪽지 API 테스트용 페이지 라우팅 (웹용)
     * - GET /chat-test
     * - 뷰 템플릿: templates/chat-test.html
     */
    public String chatTestPage() {
        return "chat-test"; // src/main/resources/templates/chat-test.html
    }

    @GetMapping("/api/chat-page")
    @ResponseBody
    /**
     * 채팅 페이지 정보 API (iOS용)
     * - GET /api/chat-page
     * - 채팅 기능 접근 가능 여부와 기본 정보 반환
     */
    public ResponseEntity<Object> getChatPageInfo() {
        return ResponseEntity.ok(Map.of(
            "pageName", "쪽지함",
            "description", "사용자 간 쪽지 주고받기",
            "available", true
        ));
    }

    //토큰 만들고 json반환
    @GetMapping("/api/token")
    @ResponseBody
    /**
     * JWT AccessToken/RefreshToken 발급 API
     * - GET /api/token
     * - 인증: 반드시 JWT 기반(UsernamePasswordAuthenticationToken + JwtUserDetails)
     * - 동작
     *   1) SecurityContext에서 provider/socialId 추출
     *   2) DB의 사용자 조회
     *   3) RefreshToken은 기존 값 우선 사용(없으면 생성/저장), AccessToken은 매호출마다 새로 발급
     *   4) TokenResponse(AT/RT/만료/사용자정보) 반환
     */
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
            String accessToken = jwtUtil.generateAccessToken(user.getUsernum(), provider, socialId , user.getIsRegistered());
            
            // TokenResponse 생성
            TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
                user.getUsernum(),
                user.getNickname(),
                user.getEmail(),
                user.getProvider(),
                user.getProfileurl(),
                user.getIsRegistered()
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
    /**
     * JWT 토큰 재발급 API(AT/RT 동시 갱신)
     * - POST /api/token/refresh
     * - 요청 헤더: Authorization: Bearer <RefreshToken>
     * - 동작
     *   1) RT 유효성 검증 및 RT 타입 확인
     *   2) RT의 userNum으로 사용자 조회 후 DB에 저장된 RT와 일치하는지 확인
     *   3) 새 AT/RT 발급 및 RT 저장, TokenResponse 반환
     * - 오류 상황: 401(검증 실패/불일치), 404(사용자 없음)
     */
    public ResponseEntity<TokenResponse> refreshToken(@RequestHeader("Authorization") String refreshTokenHeader) {
        System.out.println("=== 토큰 재발급 API 호출 ===");
        System.out.println("RefreshToken Header: " + (refreshTokenHeader != null ? refreshTokenHeader.substring(0, Math.min(20, refreshTokenHeader.length())) + "..." : "null"));
        
        if (refreshTokenHeader == null || !refreshTokenHeader.startsWith("Bearer ")) {
            System.out.println("❌ 토큰 재발급 실패: Authorization 헤더 형식 오류");
            return ResponseEntity.status(401).build();
        }
        
        String refreshToken = refreshTokenHeader.substring(7);
        System.out.println("RefreshToken 추출: " + refreshToken.substring(0, Math.min(20, refreshToken.length())) + "...");
        
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            System.out.println("❌ 토큰 재발급 실패: RefreshToken 유효성 검증 실패");
            return ResponseEntity.status(401).build();
        }
        
        try {
            Long userNum = jwtUtil.getUserNumFromToken(refreshToken);
            System.out.println("사용자 번호 추출: " + userNum);
            
            Optional<User> userOptional = appUserRepository.findById(userNum);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                System.out.println("사용자 정보 조회 성공: " + user.getNickname() + " (" + user.getProvider() + ")");
                
                // DB에 저장된 RefreshToken과 비교
                if (!refreshToken.equals(user.getRefreshToken())) {
                    System.out.println("❌ 토큰 재발급 실패: DB에 저장된 RefreshToken과 불일치");
                    return ResponseEntity.status(401).build();
                }
                
                // 새로운 토큰 생성
                String newAccessToken = jwtUtil.generateAccessToken(user.getUsernum(), user.getProvider(), user.getUserid() , user.getIsRegistered());
                String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsernum());
                
                System.out.println("새로운 토큰 생성 완료:");
                System.out.println("  - AccessToken: " + newAccessToken.substring(0, Math.min(20, newAccessToken.length())) + "...");
                System.out.println("  - RefreshToken: " + newRefreshToken.substring(0, Math.min(20, newRefreshToken.length())) + "...");
                
                // 새로운 RefreshToken을 DB에 저장
                user.setRefreshToken(newRefreshToken);
                appUserRepository.save(user);
                System.out.println("✅ 새로운 RefreshToken DB 저장 완료");
                
                // TokenResponse 생성
                TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
                    user.getUsernum(),
                    user.getNickname(),
                    user.getEmail(),
                    user.getProvider(),
                    user.getProfileurl(),
                    user.getIsRegistered()
                );
                
                TokenResponse tokenResponse = new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    1800, // 30분 (초 단위)
                    604800, // 7일 (초 단위)
                    userInfo
                );
                
                System.out.println("✅ 토큰 재발급 성공 - 사용자: " + user.getNickname());
                return ResponseEntity.ok(tokenResponse);
            } else {
                System.out.println("❌ 토큰 재발급 실패: 사용자를 찾을 수 없음 (userNum: " + userNum + ")");
                return ResponseEntity.status(404).build();
            }
        } catch (Exception e) {
            System.out.println("❌ 토큰 재발급 실패: 예외 발생");
            System.out.println("에러 메시지: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).build();
        }
    }
    // 통합된 사용자 정보 저장 (신규/기존 모두 처리) - REST API용
    // 전용함수추가
    @PostMapping(value = "/api/user-profile", consumes = {"multipart/form-data"})
    @ResponseBody
    public ResponseEntity<User> updateUserProfile(
            @RequestPart("user") User user,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

        try {
            // SecurityContext에서 인증 정보 가져오기
            UsernamePasswordAuthenticationToken authentication =
                    (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

            JwtAuthenticationFilter.JwtUserDetails details =
                    (JwtAuthenticationFilter.JwtUserDetails) authentication.getDetails();

            // 토큰에서 꺼낸 socialId와 provider를 user 객체에 설정
            user.setUserid(details.getSocialId());
            user.setProvider(details.getProvider());

            userService.updateUser(user, profileImage);

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    // 카카오 로그인 (프론트엔드 토큰 방식)
    @PostMapping("/api/auth/kakao")
    @ResponseBody
    /**
     * 카카오 로그인 API (프론트엔드에서 카카오 액세스 토큰을 받아서 처리)
     * - POST /api/auth/kakao
     * - 요청 바디: { accessToken: String }
     * - 동작
     *   1) 카카오 액세스 토큰으로 카카오 API 호출하여 사용자 정보 조회
     *   2) DB에 사용자 정보 저장/업데이트
     *   3) JWT 토큰 생성하여 반환
     * - 오류 상황: 400(잘못된 요청), 401(카카오 토큰 무효), 500(서버 오류)
     */
    public ResponseEntity<TokenResponse> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        try {
            // 요청 데이터 유효성 검사
            if (request == null || request.getAccessToken() == null || request.getAccessToken().trim().isEmpty()) {
                return ResponseEntity.badRequest().build(); // 400 Bad Request
            }

            // 1. 카카오 액세스 토큰으로 카카오 API 호출하여 사용자 정보 조회
            KakaoAuthService.KakaoUserInfo kakaoUserInfo = kakaoAuthService.getKakaoUserInfo(request.getAccessToken());
            if (kakaoUserInfo == null) {
                return ResponseEntity.status(401).build(); // 401 Unauthorized - 카카오 토큰이 무효하거나 만료됨
            }

            // 2. 카카오 사용자 정보를 바탕으로 DB에 사용자 정보 저장/업데이트
            // - 기존 사용자: 정보 업데이트
            // - 신규 사용자: 새로 생성
            User user = kakaoAuthService.processKakaoLogin(kakaoUserInfo);


            boolean registered = user.getIsRegistered() != null && user.getIsRegistered();
            // 3. JWT 토큰 생성 (AccessToken + RefreshToken)
            String accessToken = jwtUtil.generateAccessToken(
                    user.getUsernum(),
                    "kakao",
                    user.getUserid() ,
                    registered
            );
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsernum());

            // 4. RefreshToken을 DB에 저장 (토큰 재발급 시 검증용)
            user.setRefreshToken(refreshToken);
            appUserRepository.save(user);

            // 5. 클라이언트에 반환할 TokenResponse 객체 생성
            TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
                user.getUsernum(),    // 사용자 고유 번호
                user.getNickname(),   // 서비스 내 닉네임
                user.getEmail(),      // 이메일
                user.getProvider(),   // "kakao"
                user.getProfileurl(), // 프로필 이미지 URL
                user.getIsRegistered()
            );
            TokenResponse tokenResponse = new TokenResponse(
                accessToken,          // JWT AccessToken (30분)
                refreshToken,         // JWT RefreshToken (7일)
                1800,                 // AccessToken 만료 시간 (30분, 초 단위)
                604800,               // RefreshToken 만료 시간 (7일, 초 단위)
                userInfo              // 사용자 정보
            );
            System.out.println("======================"+user.getIsRegistered()+"======================");
            System.out.println("======================"+user.getName()+"======================");
            return ResponseEntity.ok(tokenResponse); // 200 OK

        } catch (Exception e) {
            // 예외 발생 시 로그 기록 및 500 에러 반환
            System.err.println("카카오 로그인 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build(); // 500 Internal Server Error
        }
    }
}
