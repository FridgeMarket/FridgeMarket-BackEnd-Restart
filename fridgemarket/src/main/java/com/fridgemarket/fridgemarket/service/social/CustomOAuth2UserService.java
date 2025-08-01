package com.fridgemarket.fridgemarket.service.social;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
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
        String socialId = oauth2User.getName(); // This is the unique ID from the provider

        // 1. Extract user info based on provider
        if ("google".equals(registrationId)) {
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                }
            }
        }

        if (socialId == null || socialId.isEmpty()) {
            throw new IllegalArgumentException("Social ID cannot be null or empty from " + registrationId);
        }

        String provider = userRequest.getClientRegistration().getRegistrationId();

        // 2. Find or create user
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        User appUser;
        if (userOptional.isPresent()) {
            appUser = userOptional.get();
        } else {
            appUser = new User();
            appUser.setUserid(socialId);
            appUser.setProvider(provider);
            appUser.setEmail(email);
            appUser.setName(name);
            appUser.setNickname(null);
            appUser.setPhone("");
            appUser.setAddress("");
            appUser.setAgreed(false);
        }

        // 4. Save the user (either updated or new)
        appUserRepository.save(appUser);

        // 5. Return the principal (OAuth2User)
        Map<String, Object> userAttributes = new HashMap<>(oauth2User.getAttributes());
        userAttributes.put("provider", provider);
        userAttributes.put("socialId", socialId);
        userAttributes.put("id", socialId);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                "id" // nameAttributeKey
        );
    }
}
