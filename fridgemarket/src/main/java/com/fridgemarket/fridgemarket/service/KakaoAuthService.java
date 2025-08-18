package com.fridgemarket.fridgemarket.service;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import com.fridgemarket.fridgemarket.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthService {

    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 카카오 액세스 토큰을 사용하여 카카오 API에서 사용자 정보를 조회
     * 
     * @param accessToken iOS 앱에서 받은 카카오 액세스 토큰
     * @return 카카오 사용자 정보 (실패 시 null)
     */
    public KakaoUserInfo getKakaoUserInfo(String accessToken) {
        try {
            // 카카오 API 호출을 위한 HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken); // Bearer 토큰 방식으로 인증
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 카카오 사용자 정보 API 호출 (https://kapi.kakao.com/v2/user/me)
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            // 응답에서 사용자 정보 추출
            Map<String, Object> userInfo = response.getBody();
            if (userInfo != null) {
                // 카카오 고유 사용자 ID (필수)
                String socialId = userInfo.get("id").toString();
                
                // 카카오 계정 정보 (선택적 - 사용자가 동의한 경우에만)
                Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
                String email = "";
                String nickname = "";
                String profileUrl = "";
                
                if (kakaoAccount != null) {
                    // 이메일 정보 (사용자가 이메일 제공에 동의한 경우)
                    email = (String) kakaoAccount.get("email");
                    
                    // 프로필 정보 (닉네임, 프로필 이미지)
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    if (profile != null) {
                        nickname = (String) profile.get("nickname");
                        profileUrl = (String) profile.get("profile_image_url");
                    }
                }
                
                return new KakaoUserInfo(socialId, email, nickname, profileUrl);
            }
        } catch (Exception e) {
            // 카카오 API 호출 실패 시 로그 기록
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
        }
        
        return null; // 실패 시 null 반환
    }

    /**
     * 카카오 사용자 정보를 바탕으로 DB에 사용자 정보를 저장하거나 업데이트
     * 
     * @param kakaoUserInfo 카카오 API에서 받은 사용자 정보
     * @return 저장/업데이트된 사용자 엔티티
     */
    public User processKakaoLogin(KakaoUserInfo kakaoUserInfo) {
        String provider = "kakao"; // 제공자 고정값
        String socialId = kakaoUserInfo.getSocialId(); // 카카오 고유 ID
        
        // DB에서 기존 사용자 조회 (provider + socialId로 식별)
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        User user;
        
        if (userOptional.isPresent()) {
            // 기존 사용자 - 정보 업데이트
            log.info("기존 카카오 사용자 로그인: {}", socialId);
            user = userOptional.get();
            
            // 카카오에서 받은 정보로 기존 정보 업데이트 (변경된 경우에만)
            if (kakaoUserInfo.getEmail() != null && !kakaoUserInfo.getEmail().equals(user.getEmail())) {
                user.setEmail(kakaoUserInfo.getEmail());
            }
            if (kakaoUserInfo.getName() != null && !kakaoUserInfo.getName().equals(user.getName())) {
                user.setName(kakaoUserInfo.getName());
            }
            // null이면 false 처리
            if (user.getIsRegistered() == null) {
                user.setIsRegistered(false);
            }
        } else {
            // 신규 사용자 - 새로 생성
            log.info("새로운 카카오 사용자 등록: {}", socialId);
            user = new User();
            user.setUserid(socialId); // 카카오 고유 ID
            user.setProvider(provider); // "kakao"
            user.setEmail(kakaoUserInfo.getEmail()); // 카카오 이메일
            user.setName(kakaoUserInfo.getName()); // 카카오 닉네임
            // 프로필 이미지 설정 (카카오 프로필이 있으면 사용, 없으면 기본 이미지)
            user.setProfileurl(kakaoUserInfo.getProfileUrl() != null ? 
                kakaoUserInfo.getProfileUrl() : "/images/FridgeMarketIcon.png");
            user.setNickname(null); // 서비스 내 닉네임은 나중에 사용자가 설정
            user.setPhone(""); // 전화번호는 나중에 입력
            user.setAddress(""); // 주소는 나중에 입력
            user.setAgreed(false); // 약관 동의 여부
            user.setAdmin(false); // 일반 사용자로 설정
            user.setIsRegistered(false);
        }
        
        // DB에 저장/업데이트
        appUserRepository.save(user);
        return user;
    }

    /**
     * 카카오 API에서 받은 사용자 정보를 담는 내부 클래스
     */
    public static class KakaoUserInfo {
        private String socialId;    // 카카오 고유 사용자 ID (필수)
        private String email;       // 카카오 이메일 (선택적 - 사용자가 동의한 경우)
        private String name;        // 카카오 닉네임 (선택적)
        private String profileUrl;  // 카카오 프로필 이미지 URL (선택적)

        public KakaoUserInfo(String socialId, String email, String name, String profileUrl) {
            this.socialId = socialId;
            this.email = email;
            this.name = name;
            this.profileUrl = profileUrl;
        }

        // Getters
        public String getSocialId() { return socialId; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getProfileUrl() { return profileUrl; }
    }
}
