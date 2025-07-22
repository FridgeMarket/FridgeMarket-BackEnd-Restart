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

        System.out.println("OAuth2User Attributes: " + oauth2User.getAttributes());

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String socialId = oauth2User.getName();
        String provider = userRequest.getClientRegistration().getRegistrationId();

        if (email == null || email.isEmpty()) {
            System.err.println("Error: Email attribute is null or empty for OAuth2 user. Attributes: " + oauth2User.getAttributes());
            throw new OAuth2AuthenticationException("Email not found or is empty in OAuth2 user attributes.");
        }

        Optional<User> appUserOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        User appUser;
        final boolean needsMoreInfo;

        if (appUserOptional.isPresent()) {
            appUser = appUserOptional.get();
            appUser.setName(name);
            appUserRepository.save(appUser);
            needsMoreInfo = false;
            System.out.println("Existing user logged in and updated: " + email);
        } else {
            appUser = new User();
            appUser.setEmail(email);
            appUser.setName(name);
            appUser.setUserid(socialId);
            appUser.setProvider(provider);
            appUser.setNickname(name != null && !name.isEmpty() ? name : email.split("@")[0]);
            appUser.setPhone("");
            appUser.setAddress("");
            appUser.setAgreed(false);
            appUserRepository.save(appUser);
            needsMoreInfo = true;
            System.out.println("New user registered: " + email);
        }

        Map<String, Object> userAttributes = new HashMap<>(oauth2User.getAttributes());
        userAttributes.put("needsMoreInfo", needsMoreInfo);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                "email"
        );
    }
}