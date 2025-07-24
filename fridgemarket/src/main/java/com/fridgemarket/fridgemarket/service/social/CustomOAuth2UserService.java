package com.fridgemarket.fridgemarket.service.social;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserRepository appUserRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email = "";
        String name = "";
        String socialId = oauth2User.getName(); // 소셜 서비스의 고유 ID (userid에 매핑될 값)

        if ("google".equals(registrationId)) {
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            // Google의 고유 ID는 보통 'sub'입니다. oauth2User.getName()이 이미 'sub'를 가져올 것입니다.
            // socialId = oauth2User.getAttribute("sub"); // 이전에 추가했지만, getName()이 더 일반적입니다.
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                }
            }
            // Kakao의 고유 ID는 'id'입니다. oauth2User.getName()이 이미 'id'를 Long 타입으로 가져와 String으로 변환합니다.
            // Object kakaoRawId = oauth2User.getAttribute("id");
            // if (kakaoRawId != null) {
            //     socialId = String.valueOf(kakaoRawId);
            // } else {
            //     socialId = oauth2User.getName(); // Fallback to getName()
            // }
        }

        if (socialId == null || socialId.isEmpty()) {
            throw new IllegalArgumentException("Social ID (unique identifier) cannot be null or empty from " + registrationId + " provider.");
        }

        String provider = userRequest.getClientRegistration().getRegistrationId();

        // appUserRepository.findByProviderAndUserid 로 변경!
        Optional<User> appUserOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        User appUser;

        if (appUserOptional.isPresent()) {
            appUser = appUserOptional.get();
            // 기존 사용자의 경우, 변경된 이름(닉네임)이 있다면 DB에 업데이트 할 수 있습니다.
            // 다만, 사용자가 직접 프로필을 수정하는 기능과 충돌할 수 있으므로 정책 결정이 필요합니다.
            // 여기서는 로그인 시마다 소셜 프로필 이름으로 강제 업데이트하는 로직을 비활성화하고,
            // 사용자가 직접 입력한 닉네임을 유지하도록 합니다.
            // appUser.setName(name);
            // appUserRepository.save(appUser);
            System.out.println("Existing user logged in: " + (email != null ? email : socialId));
        } else {
            appUser = new User();
            appUser.setEmail(email);
            appUser.setName(name); // 소셜에서 받은 이름 저장
            appUser.setUserid(socialId);
            appUser.setProvider(provider);
            appUser.setNickname(null); // ⭐ 중요: 추가 정보 입력을 위해 닉네임은 null로 설정
            appUser.setPhone("");
            appUser.setAddress("");
            appUser.setAgreed(false);
            appUserRepository.save(appUser);
            System.out.println("New user registered: " + (email != null ? email : socialId));
        }

        Map<String, Object> userAttributes = new HashMap<>(oauth2User.getAttributes());
        userAttributes.put("provider", provider);
        userAttributes.put("socialId", socialId); // 편의를 위해 "socialId"로 추가 (DB 필드명과 혼동X)

        // DefaultOAuth2User의 세 번째 인자는 attributes 맵에서 '주체(Principal)의 이름'으로 사용할 키를 지정합니다.
        // 여기서 "id"로 지정했으므로, userAttributes 맵에는 "id"라는 키가 존재해야 합니다.
        // Spring Security의 DefaultOAuth2UserService는 provider에 따라 nameAttribute를 사용하는데,
        // 카카오는 "id", 구글은 "sub"를 사용합니다. oauth2User.getName()이 이 값을 가져옵니다.
        // 따라서, userAttributes 맵에 "id" 키로 socialId를 다시 넣어주는 것이 안전합니다.
        userAttributes.put("id", socialId); // DefaultOAuth2User의 name attribute로 사용될 "id" 키에 socialId 값을 넣어줍니다.

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                "id" // DefaultOAuth2User의 getName()이 userAttributes 맵의 "id" 키 값을 반환하게 됩니다.
        );
    }
}