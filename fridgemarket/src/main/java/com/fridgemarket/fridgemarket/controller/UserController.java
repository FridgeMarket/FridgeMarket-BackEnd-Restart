package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository appUserRepository;

    // 로그인 성공 시 호출 (CustomAuthenticationSuccessHandler에서 리다이렉트)
    @GetMapping("/success")
    public String successPage(Authentication authentication, Model model) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        String socialId = token.getPrincipal().getName();

        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        if (userOptional.isEmpty()) {
            // 신규 사용자라면 userinfo로 리다이렉트 (보통 여기 오진 않음)
            return "redirect:/userinfo?socialId=" + socialId + "&provider=" + provider;
        }
        User user = userOptional.get();
        model.addAttribute("user", user);
        return "success";  // src/main/resources/templates/success.html
    }

    // 사용자 정보 입력/수정 폼
    @GetMapping("/userinfo")
    public String userInfoPage(@RequestParam String socialId,
                               @RequestParam String provider,
                               Model model) {
        Optional<User> userOptional = appUserRepository.findByProviderAndUserid(provider, socialId);
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
        } else {
            // 신규 사용자 빈 User 객체 전달
            User newUser = new User();
            newUser.setUserid(socialId);
            newUser.setProvider(provider);
            model.addAttribute("user", newUser);
        }
        return "userinfo";  // src/main/resources/templates/userinfo.html
    }

    // 사용자 정보 저장 처리 (신규/수정 모두)
    @PostMapping("/updateUserInfo")
    public String updateUserInfo(@ModelAttribute User user) {
        Optional<User> existingUserOpt = appUserRepository.findByProviderAndUserid(user.getProvider(), user.getUserid());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            // 수정할 필드만 업데이트
            existingUser.setNickname(user.getNickname());
            existingUser.setPhone(user.getPhone());
            existingUser.setAddress(user.getAddress());
            existingUser.setAgreed(user.getAgreed());
            existingUser.setBirth(user.getBirth()); // 생년월일 추가
            appUserRepository.save(existingUser);
        } else {
            // 신규 사용자 저장
            appUserRepository.save(user);
        }
        return "redirect:/success";
    }
}
