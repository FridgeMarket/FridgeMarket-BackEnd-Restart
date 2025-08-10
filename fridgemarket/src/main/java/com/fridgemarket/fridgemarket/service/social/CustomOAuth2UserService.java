package com.fridgemarket.fridgemarket.service.social;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    // Google 전용 OAuth2 서비스
    private final GoogleOAuth2UserService googleOAuth2UserService;
    // Kakao 전용 OAuth2 서비스
    private final KakaoOAuth2UserService kakaoOAuth2UserService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // ========== 1. OAuth2 제공자 식별 ==========
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        log.info("OAuth2 로그인 요청 - 제공자: {}", registrationId);


        switch (registrationId.toLowerCase()) {
            case "google": //구글일때
                log.info("Google OAuth2 서비스로 위임");
                return googleOAuth2UserService.loadUser(userRequest);
                
            case "kakao": //카카오일때
                log.info("Kakao OAuth2 서비스로 위임");
                return kakaoOAuth2UserService.loadUser(userRequest);
                
            default:
                // 오류
                log.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
                throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        }
    }
}
