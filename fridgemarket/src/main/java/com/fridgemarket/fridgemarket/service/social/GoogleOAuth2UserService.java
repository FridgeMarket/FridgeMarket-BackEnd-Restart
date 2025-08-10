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
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    // 사용자 정보 저장소 (생성자 주입)
    private final AppUserRepository appUserRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Google OAuth2 로그인 시작");
        
        // 구글 사용자 정보 호출
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // 구글에서 필요한 정보 추출
        String socialId = oauth2User.getName(); // Google의 고유 사용자 ID
        String email = oauth2User.getAttribute("email"); // 이메일 주소
        String name = oauth2User.getAttribute("name"); // 실명
        String profileUrl = oauth2User.getAttribute("picture"); // 프로필 이미지 URL
        
        log.info("Google 사용자 정보 - ID: {}, 이메일: {}, 이름: {}", socialId, email, name);
        
        // 필수값 검증
        if (socialId == null || socialId.isEmpty()) {
            throw new IllegalArgumentException("Google에서 사용자 ID를 가져올 수 없습니다.");
        }
        
        // 제공자
        String provider = "google";
        
        // provider, socialId값 DB에서 확인
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        User appUser;
        
        if (userOptional.isPresent()) {
            // 기존 사용자
            log.info("기존 Google 사용자 로그인");
            appUser = userOptional.get();
            
            // 이메일이나 이름이 변경된 경우 업데이트
            if (email != null && !email.equals(appUser.getEmail())) {
                appUser.setEmail(email);
                log.info("Google 이메일 정보 업데이트");
            }
            
            if (name != null && !name.equals(appUser.getName())) {
                appUser.setName(name);
                log.info("Google 이름 정보 업데이트");
            }
            
        } else {
            // 신규 사용자
            log.info("새로운 Google 사용자 등록");
            appUser = new User();
            appUser.setUserid(socialId); // Google 고유 ID
            appUser.setProvider(provider); // "google"
            appUser.setEmail(email); // Google 이메일
            appUser.setName(name); // Google 실명
            // 기본 프로필 이미지
            appUser.setProfileurl("/images/FridgeMarketIcon.png");
            
            // 기본값 설정 (추가 정보는 나중에 입력받음)
            appUser.setNickname(null); // 나중에 입력받을 닉네임
            appUser.setPhone(""); // 나중에 입력받을 전화번호
            appUser.setAddress(""); // 나중에 입력받을 주소
            appUser.setAgreed(false); // 약관 동의 여부 (나중에 받음)
            appUser.setAdmin(false); // 기본적으로 일반 사용자
        }
        
        // DB저장
        appUserRepository.save(appUser);
        log.info("Google 사용자 정보 데이터베이스 저장 완료");
        
        // Security에서 사용할 OAuth2User 객체 생성
        Map<String, Object> userAttributes = new HashMap<>(oauth2User.getAttributes());
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

