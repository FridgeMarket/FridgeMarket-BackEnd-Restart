package com.fridgemarket.fridgemarket.service;

import com.fridgemarket.fridgemarket.helper.constants.SocialLoginType;
import com.fridgemarket.fridgemarket.service.social.SocialOauth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final List<SocialOauth> socialOauthList;
    // private final HttpServletResponse response; // <-- 이 줄은 올바르게 주석 처리/제거되었습니다.

    /**
     * 특정 소셜 로그인 타입에 해당하는 리디렉션 URL을 반환합니다.
     * @param socialLoginType 소셜 로그인 타입 (Google, Naver 등)
     * @return 소셜 로그인 제공자의 인증 페이지로 리디렉션할 URL
     */
    public String getRedirectUrl(SocialLoginType socialLoginType) {
        SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType);
        // ⭐ 수정: 인터페이스와 일치하도록 getOauthRedirectUrl()로 변경 ⭐
        return socialOauth.getOauthRedirectUrl(); // 수정된 메서드 호출
    }

    /**
     * 인가 코드를 사용하여 소셜 로그인 제공자로부터 액세스 토큰을 요청합니다.
     * @param socialLoginType 소셜 로그인 타입
     * @param code 인가 코드
     * @return 발급받은 액세스 토큰
     */
    public String requestAccessToken(SocialLoginType socialLoginType, String code) {
        SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType);
        return socialOauth.requestAccessToken(code);
    }

    /**
     * 주어진 소셜 로그인 타입에 해당하는 SocialOauth 구현체를 찾습니다.
     * @param socialLoginType 찾을 소셜 로그인 타입
     * @return 해당 SocialOauth 구현체
     * @throws IllegalArgumentException 알 수 없는 SocialLoginType인 경우
     */
    private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
        return socialOauthList.stream()
                .filter(x -> x.type() == socialLoginType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 SocialLoginType 입니다."));
    }
}
