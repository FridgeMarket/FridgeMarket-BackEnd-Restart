package com.fridgemarket.fridgemarket.service;

import com.fridgemarket.fridgemarket.DAO.Fridge;
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.dto.FridgeDto;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import com.fridgemarket.fridgemarket.repository.FridgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FridgeService {
    
    private final FridgeRepository fridgeRepository;
    private final AppUserRepository appUserRepository;
    
    // 재료 추가
    public FridgeDto addFood(Long userNum, FridgeDto.CreateRequest request) {
        Optional<User> userOptional = appUserRepository.findById(userNum);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        User user = userOptional.get();
        Fridge fridge = new Fridge();
        fridge.setUser(user);
        fridge.setFoodname(request.getFoodname());
        fridge.setTag(request.getTag());
        fridge.setAmount(request.getAmount());
        fridge.setRegisteredat(new Date());
        fridge.setExpirationdate(request.getExpirationdate());
        fridge.setUnit(request.getUnit());
        
        Fridge savedFridge = fridgeRepository.save(fridge);
        return convertToDto(savedFridge);
    }
    
    // 사용자의 모든 재료 조회
    public List<FridgeDto> getUserFoods(Long userNum) {
        List<Fridge> fridges = fridgeRepository.findByUser_Usernum(userNum);
        return fridges.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // 특정 재료 조회
    public FridgeDto getFood(Long userNum, Long fridgeNum) {
        Optional<Fridge> fridgeOptional = fridgeRepository.findById(fridgeNum);
        if (fridgeOptional.isEmpty()) {
            throw new RuntimeException("재료를 찾을 수 없습니다.");
        }
        
        Fridge fridge = fridgeOptional.get();
        if (!fridge.getUser().getUsernum().equals(userNum)) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }
        
        return convertToDto(fridge);
    }
    
    // 재료 수정
    public FridgeDto updateFood(Long userNum, Long fridgeNum, FridgeDto.UpdateRequest request) {
        System.out.println("=== FridgeService.updateFood 호출 ===");
        System.out.println("UserNum: " + userNum + ", FridgeNum: " + fridgeNum);
        System.out.println("Request: " + request.getFoodname() + ", " + request.getTag() + ", " + request.getAmount());
        
        Optional<Fridge> fridgeOptional = fridgeRepository.findById(fridgeNum);
        if (fridgeOptional.isEmpty()) {
            System.out.println("❌ 재료 수정 실패: 재료를 찾을 수 없음 (fridgeNum: " + fridgeNum + ")");
            throw new RuntimeException("재료를 찾을 수 없습니다.");
        }
        
        Fridge fridge = fridgeOptional.get();
        System.out.println("재료 정보 조회: " + fridge.getFoodname() + " (소유자: " + fridge.getUser().getUsernum() + ")");
        
        if (!fridge.getUser().getUsernum().equals(userNum)) {
            System.out.println("❌ 재료 수정 실패: 접근 권한 없음 (요청자: " + userNum + ", 소유자: " + fridge.getUser().getUsernum() + ")");
            throw new RuntimeException("접근 권한이 없습니다.");
        }
        
        System.out.println("재료 정보 업데이트 중...");
        // 부분 업데이트: null이 아닌 필드만 업데이트
        if (request.getFoodname() != null) {
            System.out.println("  - 음식명 변경: " + fridge.getFoodname() + " → " + request.getFoodname());
            fridge.setFoodname(request.getFoodname());
        }
        if (request.getTag() != null) {
            System.out.println("  - 태그 변경: " + fridge.getTag() + " → " + request.getTag());
            fridge.setTag(request.getTag());
        }
        if (request.getAmount() != null) {
            System.out.println("  - 수량 변경: " + fridge.getAmount() + " → " + request.getAmount());
            fridge.setAmount(request.getAmount());
        }
        if (request.getExpirationdate() != null) {
            System.out.println("  - 유통기한 변경: " + fridge.getExpirationdate() + " → " + request.getExpirationdate());
            fridge.setExpirationdate(request.getExpirationdate());
        }
        if (request.getUnit() != null) {
            System.out.println("  - 단위 변경: " + fridge.getUnit() + " → " + request.getUnit());
            fridge.setUnit(request.getUnit());
        }
        
        Fridge updatedFridge = fridgeRepository.save(fridge);
        System.out.println("✅ 재료 수정 완료:");
        System.out.println("  - 재료번호: " + updatedFridge.getFridgenum());
        System.out.println("  - 음식명: " + updatedFridge.getFoodname());
        System.out.println("  - 태그: " + updatedFridge.getTag());
        System.out.println("  - 수량: " + updatedFridge.getAmount());
        return convertToDto(updatedFridge);
    }
    
    // 재료 삭제
    public void deleteFood(Long userNum, Long fridgeNum) {
        Optional<Fridge> fridgeOptional = fridgeRepository.findById(fridgeNum);
        if (fridgeOptional.isEmpty()) {
            throw new RuntimeException("재료를 찾을 수 없습니다.");
        }
        
        Fridge fridge = fridgeOptional.get();
        if (!fridge.getUser().getUsernum().equals(userNum)) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }
        
        fridgeRepository.delete(fridge);
    }
    
    // 태그별 재료 조회
    public List<FridgeDto> getFoodsByTag(Long userNum, String tag) {
        List<Fridge> fridges = fridgeRepository.findByUser_UsernumAndTag(userNum, tag);
        return fridges.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // 음식명으로 검색
    public List<FridgeDto> searchFoodsByName(Long userNum, String foodName) {
        List<Fridge> fridges = fridgeRepository.findByUser_UsernumAndFoodnameContaining(userNum, foodName);
        return fridges.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // DTO 변환
    private FridgeDto convertToDto(Fridge fridge) {
        return new FridgeDto(
            fridge.getFridgenum(),
            fridge.getFoodname(),
            fridge.getTag(),
            fridge.getAmount(),
            fridge.getRegisteredat(),
            fridge.getExpirationdate(),
            fridge.getUnit()
        );
    }
}
