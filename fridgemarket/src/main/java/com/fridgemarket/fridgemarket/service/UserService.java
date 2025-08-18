package com.fridgemarket.fridgemarket.service;

import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.dto.UserUpdateDto;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    // 사용자 정보 저장소 (생성자 주입)
    private final AppUserRepository userRepository;

    // 파일 업로드 루트 디렉토리 설정
    private final Path rootLocation = Paths.get("uploads");

    //생성자
    public UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
        try {
            Files.createDirectories(rootLocation.resolve("profile_images"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }
    
    //provider와 socialId로 사용자를 찾는 메서드
    public Optional<User> findByProviderAndSocialId(String provider, String socialId) {
        return userRepository.findByProviderAndUserid(provider, socialId);
    }

    //userID로 찾기
    public User findByUserId(String userId) {
        return userRepository.findByUserid(userId).orElse(null);
    }

    //닉네임으로 사용자 찾기
    public User findByNickname(String nickname) {
        return userRepository.findByNickname(nickname).orElse(null);
    }

    //사용자 정보 저장 및 업데이트
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
        userToUpdate.setIsRegistered(true);
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String filename = UUID.randomUUID().toString() + "_" + profileImage.getOriginalFilename();
                Path destinationFile = this.rootLocation.resolve("profile_images").resolve(Paths.get(filename)).normalize().toAbsolutePath();
                
                profileImage.transferTo(destinationFile);
                
                userToUpdate.setProfileurl("/uploads/profile_images/" + filename);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file", e);
            }
        } else if (userToUpdate.getProfileurl() == null || userToUpdate.getProfileurl().isEmpty()) {
            // 프로필 이미지가 없고, 소셜 로그인 기본 프로필도 없는 경우에만 기본 이미지 설정
            userToUpdate.setProfileurl("/images/FridgeMarketIcon.png");
        }

        userRepository.save(userToUpdate);
    }

    //DTO를 사용하여 사용자 정보를 업데이트하는 메서드
    @Transactional
    public void updateUserInfo(UserUpdateDto userUpdateDto) {
        User user = userRepository.findByProviderAndUserid(userUpdateDto.getProvider(), userUpdateDto.getSocialId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with provider " +
                        userUpdateDto.getProvider() + " and socialId " + userUpdateDto.getSocialId()));

        user.setNickname(userUpdateDto.getNickname());
        user.setPhone(userUpdateDto.getPhone());
        user.setAddress(userUpdateDto.getAddress());
        user.setAgreed(userUpdateDto.isAgreed());
        user.setBirth(userUpdateDto.getBirth());

        userRepository.save(user);
    }