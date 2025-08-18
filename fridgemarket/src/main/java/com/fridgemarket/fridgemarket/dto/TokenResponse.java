package com.fridgemarket.fridgemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    
    // JWT AccessToken (API 호출에 사용)
    private String accessToken;
    
    // JWT RefreshToken (AccessToken 갱신에 사용)
    private String refreshToken;
    
    // AccessToken 만료 시간 (초 단위)
    private long accessTokenExpiresIn;
    
    // RefreshToken 만료 시간 (초 단위)
    private long refreshTokenExpiresIn;
    
    // 토큰 타입 (일반적으로 "Bearer")
    private String tokenType = "Bearer";
    
    // 사용자 정보
    private UserInfo user;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userNum;
        private String nickname;
        private String email;
        private String provider;
        private String profileUrl;
        private boolean isRegistered;
    }
    
    // 편의 생성자 - 토큰과 사용자 정보만으로 생성
    public TokenResponse(String accessToken, String refreshToken, 
                        long accessTokenExpiresIn, long refreshTokenExpiresIn, 
                        UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
        this.tokenType = "Bearer";
        this.user = user;
    }
}

