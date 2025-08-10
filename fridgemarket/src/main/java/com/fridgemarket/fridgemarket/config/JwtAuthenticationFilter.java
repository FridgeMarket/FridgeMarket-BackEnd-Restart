package com.fridgemarket.fridgemarket.config;

import com.fridgemarket.fridgemarket.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT 생성/검증 유틸
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // 헤더에서 토큰 꺼내기
            String token = extractTokenFromRequest(request);
            
            // 토큰이 있고 유효한지
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                // AT인지
                if (jwtUtil.isAccessToken(token)) {
                    Long userNum = jwtUtil.getUserNumFromToken(token);
                    String provider = jwtUtil.getProviderFromToken(token);
                    String socialId = jwtUtil.getSocialIdFromToken(token);
                    
                    // Spring Security 인증 객체 만들기
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            socialId, // principal (사용자 식별자)
                            null, // credentials (비밀번호 - JWT에서는 불필요)
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")) // 권한 설정
                    );
                    
                    // ========== 6. 추가 사용자 정보를 authentication에 저장 ==========
                    authentication.setDetails(new JwtUserDetails(userNum, provider, socialId));
                    
                    // ========== 7. SecurityContext에 인증 정보 설정 ==========
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.info("JWT 토큰 인증 성공 - 사용자: {}, 제공자: {}", socialId, provider);
                } else {
                    // RefreshToken이 전송된 경우 경고 로그
                    log.warn("AccessToken이 아닌 토큰이 전송됨");
                }
            }
        } catch (Exception e) {
            // JWT 토큰 처리 중 예외 발생 시 로그 기록
            log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
        }
        
        // 필터 체인 계속 진행 (인증 실패해도 요청은 계속 처리)
        filterChain.doFilter(request, response);
    }


    private String extractTokenFromRequest(HttpServletRequest request) {
        // ========== 1. Authorization 헤더에서 Bearer 토큰 추출 ==========
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거하고 토큰만 반환
        }
        return null; // 토큰을 찾지 못한 경우
    }

    public static class JwtUserDetails {
        private final Long userNum;    // 사용자 고유 번호
        private final String provider; // OAuth2 제공자 (google, kakao)
        private final String socialId; // 소셜 로그인 ID

        public JwtUserDetails(Long userNum, String provider, String socialId) {
            this.userNum = userNum;
            this.provider = provider;
            this.socialId = socialId;
        }

        // Getter 메서드들
        public Long getUserNum() { return userNum; }
        public String getProvider() { return provider; }
        public String getSocialId() { return socialId; }
    }
}
