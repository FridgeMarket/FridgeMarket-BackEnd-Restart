package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.service.social.GoogleOauth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/google")
public class OauthController {

    private final GoogleOauth googleOauth;

    @GetMapping("/login")
    public String googleLogin() {
        return "redirect:" + googleOauth.getOauthRedirectUrl();
    }

    @GetMapping("/callback")
    public String googleCallback(@RequestParam(name = "code") String code) {
        googleOauth.processUser(code);
        return "redirect:/success";
    }
}
