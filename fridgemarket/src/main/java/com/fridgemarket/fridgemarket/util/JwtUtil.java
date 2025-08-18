package com.fridgemarket.fridgemarket.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    // application.yml에서 JWT 설정값들을 주입받음
    @Value("${jwt.secret}")
    private String secretKey; // JWT 서명에 사용할 비밀키
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration; // AccessToken 만료 시간 (30분)
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // RefreshToken 만료 시간 (7일)
    
    //JWT 서명에 사용할 SecretKey를 생성하는 메서드
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    //AT생성
    public String generateAccessToken(Long userNum, String provider, String socialId , boolean isRegistered) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(userNum.toString()) // 사용자 고유 번호를 subject로 설정
                .claim("provider", provider) // OAuth2 제공자 (google, kakao)
                .claim("socialId", socialId) // 소셜 로그인 ID
                .claim("isRegistered" , isRegistered)
                .claim("type", "access") // 토큰 타입 구분
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiration) // 만료 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // HS256 알고리즘으로 서명
                .compact();
    }

    //RT생성
    public String generateRefreshToken(Long userNum) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .setSubject(userNum.toString()) // 사용자 고유 번호만 포함 (보안상 최소 정보)
                .claim("type", "refresh") // 토큰 타입 구분
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiration) // 만료 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // HS256 알고리즘으로 서명
                .compact();
    }

    //JWT 토큰에서 사용자 번호를 추출하는 메서드
    public Long getUserNumFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    //JWT 토큰에서 제공자 정보를 추출하는 메서드
    public String getProviderFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("provider", String.class);
    }

    //JWT 토큰에서 소셜 ID를 추출하는 메서드
    public String getSocialIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("socialId", String.class);
    }

    //JWT 토큰에서 토큰 타입을 추출하는 메서드
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    //JWT 토큰에서 Claims를 추출하는 메서드 (토큰 파싱)
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 서명 검증용 키 설정
                .build()
                .parseClaimsJws(token) // 토큰 파싱 및 서명 검증
                .getBody();
    }

    //JWT 토큰의 유효성을 검증하는 메서드
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token); // 토큰 파싱 시도
            return true; // 파싱 성공 시 유효한 토큰
        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT 토큰입니다: {}", e.getMessage());
        } catch (SecurityException e) {
            log.error("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있거나 null입니다: {}", e.getMessage());
        }
        return false; // 예외 발생 시 무효한 토큰
    }

    //AT확인
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenTypeFromToken(token));
    }

    //RT확인
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenTypeFromToken(token));
    }

    //토큰 만료 시간까지 남은 시간을 계산하는 메서드
    public long getRemainingTime(String token) {
        Claims claims = getClaimsFromToken(token);
        Date expiration = claims.getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
}

