package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.service.KakaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/kakao")
public class KakaoLoginController {

    private final KakaoService kakaoService;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("location", kakaoService.getKakaoLogin());
        return "kakaologin";
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code, Model model) {
        String accessToken = kakaoService.getAccessTokenFromKakao(code);
        User user = kakaoService.loginWithKakao(accessToken);
        model.addAttribute("user", user);
        return "redirect:/success";
    }
}
