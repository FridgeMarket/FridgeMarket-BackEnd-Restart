package com.fridgemarket.fridgemarket.config;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import com.fridgemarket.fridgemarket.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // DB의 사용자 정보 조회/저장
    private final AppUserRepository appUserRepository;
    // OAuth2 클라이언트(구글, 카카오) 인증 정보 관리
    private final OAuth2AuthorizedClientService authorizedClientService;
    // JWT 토큰 생성 유틸
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        System.out.println("=== OAuth2 로그인 성공 핸들러 시작 ===");

        try {
            // 1) 로그인한 사용자의 인증 정보를 OAuth2 타입으로 변환
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // 로그인 제공자 이름(ex: google, kakao)
            String provider = authToken.getAuthorizedClientRegistrationId();
            // 소셜 서비스에서 부여한 고유 사용자 ID
            String socialId = oauth2User.getName();

            System.out.println("로그인 제공자: " + provider + ", 사용자 ID: " + socialId);

            // 2) 소셜 로그인에서 발급된 Refresh Token 가져오기 (있으면)
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    provider,    // google, kakao
                    authToken.getName()
            );

            String socialRefreshToken = null;
            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                socialRefreshToken = authorizedClient.getRefreshToken().getTokenValue();
                System.out.println("소셜 RefreshToken 획득 성공 (길이: " + socialRefreshToken.length() + ")");
            } else {
                System.out.println("소셜 RefreshToken 없음");
            }

            // 3) DB에 해당 소셜 사용자가 있는지 확인
            Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);

            if (userOptional.isPresent()) {
                // ===== 기존 사용자 =====
                System.out.println("기존 사용자 로그인");

                User existingUser = userOptional.get();

                // JWT AccessToken / RefreshToken 생성
                String accessToken = jwtUtil.generateAccessToken(existingUser.getUsernum(), provider, socialId);
                String refreshToken = jwtUtil.generateRefreshToken(existingUser.getUsernum());

                // DB에 JWT RefreshToken 저장
                existingUser.setRefreshToken(refreshToken);
                appUserRepository.save(existingUser);
                
                //AT, RT 로그 출력
                System.out.println("JWT 발급 완료");
                System.out.println("AccessToken: " + accessToken);
                System.out.println("RefreshToken: " + refreshToken);

                // 메인 페이지로 이동
                response.sendRedirect("/success");

            } else {
                // ===== 신규 사용자 =====
                System.out.println("신규 사용자 → 추가 정보 입력 페이지로 이동");
                //회원 정보 입력란으로 이동
                String redirectUrl = "/additional-info-form?socialId=" + socialId + "&provider=" + provider;
                response.sendRedirect(redirectUrl);
            }

        } catch (Exception e) {
            // 예외 발생 시 기본 페이지로 이동
            System.err.println("OAuth2 로그인 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            // 예외 발생 시 이동 페이지
            response.sendRedirect("/success");
        }

        System.out.println("=== OAuth2 로그인 성공 핸들러 종료 ===");
    }
}
