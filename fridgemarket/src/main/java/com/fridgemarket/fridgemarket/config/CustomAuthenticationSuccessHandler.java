package com.fridgemarket.fridgemarket.config;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
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

    private final AppUserRepository appUserRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        System.out.println("AuthenticationSuccessHandler: 진입");
        try {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            String provider = authToken.getAuthorizedClientRegistrationId();
            String socialId = oauth2User.getName();

            System.out.println("Provider: " + provider + ", Social ID: " + socialId);

            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    authToken.getAuthorizedClientRegistrationId(),
                    authToken.getName()
            );

            String refreshToken = null;
            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                refreshToken = authorizedClient.getRefreshToken().getTokenValue();
                System.out.println("RefreshToken 획득 성공");
            } else {
                System.out.println("RefreshToken 획득 실패: authorizedClient 또는 refreshToken이 null입니다.");
            }

            Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);

            if (userOptional.isPresent()) {
                System.out.println("기존 사용자 확인");
                User user = userOptional.get();
                if (refreshToken != null) {
                    user.setRefreshToken(refreshToken);
                    System.out.println("DB에 RefreshToken 저장 시도");
                    appUserRepository.save(user);
                    System.out.println("DB에 RefreshToken 저장 완료");
                }
                System.out.println("'/success'로 리디렉션 시도");
                response.sendRedirect("/success");
            } else {
                System.out.println("신규 사용자 확인");
                System.out.println("'/userinfo'로 리디렉션 시도");
                response.sendRedirect("/userinfo?socialId=" + socialId + "&provider=" + provider);
            }
        } catch (Exception e) {
            System.err.println("AuthenticationSuccessHandler 내에서 예외 발생: " + e.getMessage());
            e.printStackTrace();
            // response.sendRedirect("/loginFailure"); // 예외 발생 시 loginFailure로 리다이렉트 제거
        }
    }
}
