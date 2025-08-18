package com.fridgemarket.fridgemarket.service.social;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    // 사용자 정보 저장소 (생성자 주입)
    private final AppUserRepository appUserRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Kakao OAuth2 로그인 시작");
        
        //엑세스 토큰으로 사용자 정보 호출
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // 정보 추출
        String socialId = oauth2User.getName(); // Kakao의 고유 사용자 ID
        String email = "";
        String name = "";
        String profileUrl = "";
        
        // 카카오 프로필은 중첩 구조: kakao_account > profile
        Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
        
        if (kakaoAccount != null) {
            // 이메일 정보 추출 (사용자가 이메일 제공에 동의한 경우에만)
            email = (String) kakaoAccount.get("email");
            
            // 프로필
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            
            if (profile != null) {
                // 닉네임
                name = (String) profile.get("nickname");
                // 프로필 이미지 URL
                profileUrl = (String) profile.get("profile_image_url");
            }
        }
        
        log.info("Kakao 사용자 정보 - ID: {}, 이메일: {}, 닉네임: {}", socialId, email, name);
        
        // 정보 검증
        if (socialId == null || socialId.isEmpty()) {
            throw new IllegalArgumentException("Kakao에서 사용자 ID를 가져올 수 없습니다.");
        }
        
        // 제공자
        String provider = "kakao";
        
        // db확인
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        User appUser;
        
        if (userOptional.isPresent()) {
            // 기존 사용자
            log.info("기존 Kakao 사용자 로그인");
            appUser = userOptional.get();
            
            // 프로필 정보는 업데이트하지 않음 (사용자가 직접 설정한 프로필 유지)
            // Kakao에서 제공하는 기본 프로필은 무시하고, 사용자가 설정한 프로필 또는 기본 이미지 사용
            
            // 이메일이나 닉네임이 변경된 경우 업데이트
            if (email != null && !email.equals(appUser.getEmail())) {
                appUser.setEmail(email);
                log.info("Kakao 이메일 정보 업데이트");
            }
            
            if (name != null && !name.equals(appUser.getName())) {
                appUser.setName(name);
                log.info("Kakao 닉네임 정보 업데이트");
            }
            
        } else {
            // 신규사용자
            log.info("새로운 Kakao 사용자 등록");
            appUser = new User();
            appUser.setUserid(socialId); // Kakao 고유 ID
            appUser.setProvider(provider); // "kakao"
            appUser.setEmail(email); // Kakao 이메일 (제공된 경우)
            appUser.setName(name); // Kakao 닉네임
            // 기본 프로필 이미지 설정 (FridgeMarketIcon.png)
            appUser.setProfileurl("/images/FridgeMarketIcon.png");
            
            // 기본값 설정 (추가 정보는 나중에 입력받음)
            appUser.setNickname(null); // 나중에 입력받을 서비스 닉네임 (Kakao 닉네임과 별개)
            appUser.setPhone(""); // 나중에 입력받을 전화번호
            appUser.setAddress(""); // 나중에 입력받을 주소
            appUser.setAgreed(false); // 약관 동의 여부 (나중에 받음)
            appUser.setAdmin(false); // 기본적으로 일반 사용자
        }
        
        // db저장
        appUserRepository.save(appUser);
        log.info("Kakao 사용자 정보 데이터베이스 저장 완료");
        
        // Security에서 사용할 OAuth2User 객체 생성
        Map<String, Object> userAttributes = new HashMap<>(oauth2User.getAttributes());
        // 추가 속성 설정 (나중에 다른 컴포넌트에서 사용)
        userAttributes.put("provider", provider);
        userAttributes.put("socialId", socialId);
        userAttributes.put("id", socialId);
        
        // DefaultOAuth2User 객체 반환 (Spring Security Context에 저장됨)
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), // 권한 설정
                userAttributes, // 사용자 속성
                "id" // 사용자 식별자로 사용할 속성명
        );
    }
}

