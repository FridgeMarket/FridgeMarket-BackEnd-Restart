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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (개발 중일 경우)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/oauth2/**", "/error", "/auth/kakao/callback", "/kakao/**", "/google/**").permitAll() // /google/** 경로 추가
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .defaultSuccessUrl("/success", true)
                        .failureUrl("/loginFailure")
                );

        return http.build();
    }
}
