package com.fridgemarket.fridgemarket.config;

import com.fridgemarket.fridgemarket.service.social.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (개발 중일 경우)
                .authorizeHttpRequests(authorize -> authorize
                        // OAuth2 인증 시작 및 콜백 경로는 Spring Security가 처리하도록 permitAll()에 포함
                        // '/oauth2/**'가 /oauth2/authorization/* 와 /login/oauth2/code/* 를 포함합니다.
                        // 따라서 '/auth/kakao/callback', '/kakao/**', '/google/**'는 불필요하거나 충돌을 일으킬 수 있습니다.
                        .requestMatchers("/", "/login", "/oauth2/**", "/error", "/loginFailure").permitAll()
                        .requestMatchers("/userinfo", "/updateUserInfo", "/success").authenticated()
                        // 불필요하거나 충돌을 일으킬 수 있는 경로 제거: "/auth/kakao/callback", "/kakao/**", "/google/**"
                        // 만약 /kakao/login 과 같은 특정 경로가 있다면, 해당 경로만 명시적으로 permitAll() 하거나,
                        // /oauth2/authorization/kakao 로 직접 리다이렉트하는 방식으로 변경하는 것이 좋습니다.
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/loginFailure")
                );

        return http.build();
    }
}

