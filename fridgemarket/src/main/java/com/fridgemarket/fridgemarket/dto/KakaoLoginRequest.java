package com.fridgemarket.fridgemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequest {
    // iOS 앱에서 카카오 SDK로 로그인 후 받은 액세스 토큰
    // 이 토큰을 서버로 전송하여 카카오 API에서 사용자 정보를 조회하고 JWT 토큰을 발급받음
    private String accessToken; // 프론트엔드에서 받은 카카오 액세스 토큰
}
