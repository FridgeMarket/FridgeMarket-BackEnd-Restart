package com.fridgemarket.fridgemarket.service;
// ... (기존 import와 코드 유지)
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.dto.UserUpdateDto; // UserUpdateDto 임포트
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User; // OAuth2User 임포트
import org.springframework.stereotype.Service;

import java.util.Optional; // Optional 임포트

@Service
public class UserService {

    private final AppUserRepository userRepository;

    public UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ... (기존 isUserExists, saveOrUpdateUser 메서드 유지)

    // provider와 socialId로 사용자를 찾는 메서드
    public Optional<User> findByProviderAndSocialId(String provider, String socialId) {
        return userRepository.findByProviderAndUserid(provider, socialId);
    }

    public User findByUserId(String userId) {
        return userRepository.findByUserid(userId).orElse(null);
    }

    @Transactional
    public void updateUserInfo(UserUpdateDto userUpdateDto) {
        // DTO에서 provider와 socialId를 가져와서 사용자를 찾습니다.
        User user = userRepository.findByProviderAndUserid(userUpdateDto.getProvider(), userUpdateDto.getSocialId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with provider " +
                        userUpdateDto.getProvider() + " and socialId " + userUpdateDto.getSocialId()));

        // DTO의 정보로 사용자 엔티티를 업데이트합니다.
        user.setNickname(userUpdateDto.getNickname());
        user.setPhone(userUpdateDto.getPhone());
        user.setAddress(userUpdateDto.getAddress());
        user.setAgreed(userUpdateDto.isAgreed());
        user.setBirth(userUpdateDto.getBirth()); // 생년월일 추가

        userRepository.save(user);
    }
}