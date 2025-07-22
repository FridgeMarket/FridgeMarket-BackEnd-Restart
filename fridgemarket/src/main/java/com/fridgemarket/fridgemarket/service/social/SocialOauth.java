package com.fridgemarket.fridgemarket.service.social;

import com.fridgemarket.fridgemarket.helper.constants.SocialLoginType;

public interface SocialOauth {
    // 원하는 명명 규칙(camelCase)이라고 가정하고 이 메서드를 유지합니다.
    String getOauthRedirectUrl();

    String requestAccessToken(String code);

    void processUser(String code);

    // 이 default 메서드는 SocialLoginType을 결정하는 데 유용합니다.
    default SocialLoginType type() {
        if (this instanceof GoogleOauth) {
            return SocialLoginType.GOOGLE;
        }
        // 다른 타입을 처리하거나, 알 수 없는 타입인 경우 예외를 던지도록 처리할 수 있습니다.
        return null; // 또는 throw new UnsupportedOperationException("이 구현체에 대한 알 수 없는 SocialLoginType입니다.");
    }

    // 제거됨: String getOauthRedirectURL(); // 이 부분이 중복이었습니다.
}
