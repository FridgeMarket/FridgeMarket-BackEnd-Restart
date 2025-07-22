package com.fridgemarket.fridgemarket.service;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.dto.KakaoTokenResponseDto;
import com.fridgemarket.fridgemarket.dto.KakaoUserInfoResponseDto;
import com.fridgemarket.fridgemarket.repository.UserRepository;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 카카오 로그인 및 사용자 정보 처리를 담당하는 서비스 클래스.
 * 카카오 OAuth 2.0을 통해 액세스 토큰을 얻고, 이를 이용해 사용자 정보를 조회하며,
 * 애플리케이션의 User 엔티티와 연동하여 로그인 및 회원가입을 처리합니다.
 */
@Slf4j
@Service
public class KakaoService {

    private final String KAKAO_LOGIN_URL = "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code";
    private final String KAKAO_REDIRECT_URI = "http://localhost:8080/kakao/callback";

    private final String clientId;
    private final String KAUTH_TOKEN_URL_HOST = "https://kauth.kakao.com"; // 카카오 인증 서버 URL
    private final String KAUTH_USER_URL_HOST = "https://kapi.kakao.com"; // 카카오 API 서버 URL
    private final UserRepository userRepository; // 사용자 정보 저장을 위한 리포지토리

    /**
     * KakaoService의 생성자.
     * Spring의 @Value 어노테이션을 통해 application.properties에서 카카오 클라이언트 ID를 주입받고,
     * UserRepository를 주입받아 사용합니다.
     * @param clientId 카카오 애플리케이션의 클라이언트 ID
     * @param userRepository 사용자 데이터 접근을 위한 UserRepository 인스턴스
     */
    public KakaoService(@Value("${spring.security.oauth2.client.registration.kakao.client-id}") String clientId,
                        UserRepository userRepository) {
        this.clientId = clientId;
        this.userRepository = userRepository;
    }

    public String getKakaoLogin() {
        return String.format(KAKAO_LOGIN_URL, clientId, KAKAO_REDIRECT_URI);
    }

    /**
     * 카카오로부터 액세스 토큰을 발급받는 메서드.
     * 인가 코드(code)를 사용하여 카카오 인증 서버에 액세스 토큰 발급을 요청합니다.
     * @param code 카카오 인가 코드
     * @return 발급받은 액세스 토큰 문자열
     * @throws RuntimeException API 호출 실패 시 발생
     */
    public KakaoTokenResponseDto getTokenFromKakao(String code) {

        KakaoTokenResponseDto kakaoTokenResponseDto = WebClient.create(KAUTH_TOKEN_URL_HOST).post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .path("/oauth/token")
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", clientId)
                        .queryParam("code", code)
                        .build(true))
                .header(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                .retrieve()
                //TODO : Custom Exception (4xx, 5xx 에러 발생 시 예외 처리)
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new RuntimeException("Invalid Parameter")))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new RuntimeException("Internal Server Error")))
                .bodyToMono(KakaoTokenResponseDto.class)
                .block();


        log.info(" [Kakao Service] Access Token ------> {}", kakaoTokenResponseDto.getAccessToken());
        log.info(" [Kakao Service] Refresh Token ------> {}", kakaoTokenResponseDto.getRefreshToken());
        //제공 조건: OpenID Connect가 활성화 된 앱의 토큰 발급 요청인 경우 또는 scope에 openid를 포함한 추가 항목 동의 받기 요청을 거친 토큰 발급 요청인 경우
        log.info(" [Kakao Service] Id Token ------> {}", kakaoTokenResponseDto.getIdToken());
        log.info(" [Kakao Service] Scope ------> {}", kakaoTokenResponseDto.getScope());
        log.info(" [Kakao Service] getTokenFromKakao - Returning KakaoTokenResponseDto with refreshToken: {}", kakaoTokenResponseDto.getRefreshToken());

        return kakaoTokenResponseDto;
    }

    /**
     * 액세스 토큰을 사용하여 카카오 사용자 정보를 조회하는 메서드.
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 사용자 정보 응답 DTO (KakaoUserInfoResponseDto)
     * @throws RuntimeException API 호출 실패 시 발생
     */
    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {

        KakaoUserInfoResponseDto userInfo = WebClient.create(KAUTH_USER_URL_HOST)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .path("/v2/user/me")
                        .build(true))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // access token 인가
                .header(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                .retrieve()
                //TODO : Custom Exception (4xx, 5xx 에러 발생 시 예외 처리)
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new RuntimeException("Invalid Parameter")))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new RuntimeException("Internal Server Error")))
                .bodyToMono(KakaoUserInfoResponseDto.class)
                .block();

        log.info("[ Kakao Service ] Auth ID ---> {} ", userInfo.getId());
        log.info("[ Kakao Service ] NickName ---> {} ", userInfo.getKakaoAccount().getProfile().getNickName());
        log.info("[ Kakao Service ] ProfileImageUrl ---> {} ", userInfo.getKakaoAccount().getProfile().getProfileImageUrl());

        return userInfo;
    }

    /**
     * 카카오를 통한 로그인 또는 회원가입을 처리하는 메서드.
     * 액세스 토큰으로 사용자 정보를 조회한 후, 기존 사용자인지 확인하여 로그인 처리하거나
     * 신규 사용자인 경우 User 엔티티를 생성하여 저장합니다.
     * @param kakaoTokenResponseDto 카카오 토큰 응답 DTO
     * @return 로그인 또는 회원가입된 User 엔티티
     */
    @Transactional
    public User loginWithKakao(KakaoTokenResponseDto kakaoTokenResponseDto) {
        String accessToken = kakaoTokenResponseDto.getAccessToken();
        String refreshToken = kakaoTokenResponseDto.getRefreshToken();
        log.info(" [Kakao Service] loginWithKakao - Received refreshToken: {}", refreshToken);

        // 1. 액세스 토큰으로 카카오 사용자 정보 조회
        KakaoUserInfoResponseDto userInfo = getUserInfo(accessToken);
        String provider = "kakao"; // 소셜 로그인 제공자 (카카오)
        String providerId = userInfo.getId().toString(); // 카카오에서 제공하는 사용자 고유 ID

        // 2. 이미 가입된 사용자인지 확인 (provider와 providerId로 조회)
        Optional<User> UserOptional = userRepository.findByProviderAndUserid(provider, providerId);
        if (UserOptional.isPresent()) {
            // 3. 기존 사용자일 경우, 해당 User 엔티티 반환
            User existingUser = UserOptional.get();
            existingUser.setRefreshToken(refreshToken); // 기존 사용자의 refreshToken 업데이트
            log.info(" [Kakao Service] loginWithKakao - Updating existing user with refreshToken: {}", refreshToken);
            userRepository.save(existingUser);
            return existingUser;
        }

        // 4. 신규 사용자일 경우 회원가입 처리
        // User 엔티티 생성 및 필요한 정보 설정
        User newUser = new User();
        newUser.setNickname(userInfo.getKakaoAccount().getProfile().getNickName()); // 카카오 닉네임 설정
        newUser.setUserid(providerId); // 카카오 고유 ID를 사용자 ID로 설정
        newUser.setProvider(provider); // 제공자 정보 설정
        newUser.setRefreshToken(refreshToken); // refreshToken 저장
        log.info(" [Kakao Service] loginWithKakao - Creating new user with refreshToken: {}", refreshToken);
        // 필요에 따라 추가 정보 설정 (예: 이메일, 이름 등)
        newUser.setEmail(userInfo.getKakaoAccount().getEmail()); // 이메일 설정
        // newUser.setName(userInfo.getKakaoAccount().getName());

        // 5. 신규 User 엔티티 저장
        userRepository.save(newUser);
        return newUser; // 저장된 신규 User 엔티티 반환
    }
}
