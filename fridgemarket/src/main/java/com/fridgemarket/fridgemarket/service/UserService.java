package com.fridgemarket.fridgemarket.service;
// ... (기존 import와 코드 유지)
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.dto.UserUpdateDto; // UserUpdateDto 임포트
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User; // OAuth2User 임포트
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional; // Optional 임포트
import java.util.UUID;

@Service
public class UserService {

    private final AppUserRepository userRepository;

    private final Path rootLocation = Paths.get("uploads");

    public UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
        try {
            Files.createDirectories(rootLocation.resolve("profile_images"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    // ... (기존 isUserExists, saveOrUpdateUser 메서드 유지)

    // provider와 socialId로 사용자를 찾는 메서드
    public Optional<User> findByProviderAndSocialId(String provider, String socialId) {
        return userRepository.findByProviderAndUserid(provider, socialId);
    }

    @Transactional
    public void updateUser(User user, MultipartFile profileImage) {
        Optional<User> existingUserOpt = userRepository.findByProviderAndUserid(user.getProvider(), user.getUserid());
        User userToUpdate;
        if (existingUserOpt.isPresent()) {
            userToUpdate = existingUserOpt.get();
        } else {
            userToUpdate = new User();
            userToUpdate.setUserid(user.getUserid());
            userToUpdate.setProvider(user.getProvider());
        }

        userToUpdate.setNickname(user.getNickname());
        userToUpdate.setPhone(user.getPhone());
        userToUpdate.setAddress(user.getAddress());
        userToUpdate.setAgreed(user.getAgreed());
        userToUpdate.setBirth(user.getBirth());

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String filename = UUID.randomUUID().toString() + "_" + profileImage.getOriginalFilename();
                Path destinationFile = this.rootLocation.resolve("profile_images").resolve(Paths.get(filename)).normalize().toAbsolutePath();
                profileImage.transferTo(destinationFile);
                userToUpdate.setProfileurl("/uploads/profile_images/" + filename);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file", e);
            }
        } else {
            userToUpdate.setProfileurl("/images/FridgeMarketIcon.png");
        }

        userRepository.save(userToUpdate);
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