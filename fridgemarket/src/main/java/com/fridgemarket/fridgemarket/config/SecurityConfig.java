package com.fridgemarket.fridgemarket.config;

import com.fridgemarket.fridgemarket.service.social.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // JWT 검증 필터 (요청마다 토큰 검사 → 인증 컨텍스트 세팅)
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomOAuth2UserService customOAuth2UserService,
                                                   CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                                                   OAuth2AuthorizedClientService authorizedClientService) throws Exception {
        http
                // ========== 1. CSRF 보호 설정 ==========
                .csrf(csrf -> csrf.disable()) // 개발 환경에서 편의를 위해 CSRF 비활성화

                // url별 설정
                .authorizeHttpRequests(authorize -> authorize
                        // 공개 경로 (인증 불필요)
                        .requestMatchers("/", "/login", "/oauth2/**", "/error", "/loginFailure", "/check-nickname", "/posts", "/add-post", "/api/token").permitAll()
                        // 인증 필요 경로
                        .requestMatchers("/success", "/user-info").authenticated()
                        .requestMatchers( "/check-post/", "/delete-post/", "/search-post").authenticated()
                        // 기타 모든 경로 (인증 필요)
                        .anyRequest().authenticated()
                )

                //소셜로그인
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login") // 로그인 페이지
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)) // OAuth2 사용자 정보 처리 서비스
                        .successHandler(customAuthenticationSuccessHandler) // 로그인 성공 핸들러
                        .failureUrl("/loginFailure") // 로그인 실패 시 리다이렉트 URL
                        .authorizedClientService(authorizedClientService) // OAuth2 인증된 클라이언트 서비스
                )

                // 세션관리 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // OAuth2 로그인을 위해 필요할 경우 세션 만듬
                        .maximumSessions(1) // 동시 세션 수 제한
                        .maxSessionsPreventsLogin(false) // 다른 곳에서 로그인을 하면 이전 로그인 위치는 로그아웃됨
                )

                // ID/PW 인증 필터(폼 로그인)보다 먼저 우리 JWT 필터를 실행합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터를 기본 인증 필터보다 먼저 실행

        return http.build();
    }
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }
}

