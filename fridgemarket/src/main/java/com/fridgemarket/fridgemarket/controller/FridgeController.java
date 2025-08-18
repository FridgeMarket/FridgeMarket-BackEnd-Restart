package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter;
import com.fridgemarket.fridgemarket.dto.FridgeDto;
import com.fridgemarket.fridgemarket.service.FridgeService;
import com.fridgemarket.fridgemarket.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FridgeController {
    
    private final FridgeService fridgeService;
    private final JwtUtil jwtUtil;
    
    // 재료 추가
    @PostMapping("/add-food")
    public ResponseEntity<FridgeDto> addFood(@RequestBody FridgeDto.CreateRequest request) {
        try {
            System.out.println("=== addFood 메서드 호출 ===");
            System.out.println("Request: " + request.getFoodname() + ", " + request.getTag() + ", " + request.getAmount());
            
            Long userNum = getCurrentUserNum();
            System.out.println("UserNum: " + userNum);
            
            FridgeDto result = fridgeService.addFood(userNum, request);
            System.out.println("Success: " + result.getFridgenum());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 사용자의 모든 재료 조회
    @GetMapping("/check-fridge")
    public ResponseEntity<List<FridgeDto>> getUserFoods() {
        try {
            Long userNum = getCurrentUserNum();
            List<FridgeDto> foods = fridgeService.getUserFoods(userNum);
            return ResponseEntity.ok(foods);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 특정 재료 조회
    @GetMapping("/check-fridge/{fridgeNum}")
    public ResponseEntity<FridgeDto> getFood(@PathVariable Long fridgeNum) {
        try {
            Long userNum = getCurrentUserNum();
            FridgeDto food = fridgeService.getFood(userNum, fridgeNum);
            return ResponseEntity.ok(food);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // 재료 수정
    @PatchMapping("/update-fridge/{fridgeNum}")
    public ResponseEntity<FridgeDto> updateFood(@PathVariable Long fridgeNum, 
                                               @RequestBody FridgeDto.UpdateRequest request) {
        try {
            System.out.println("=== 재료 수정 API 호출 ===");
            System.out.println("FridgeNum: " + fridgeNum);
            System.out.println("Request: " + request.getFoodname() + ", " + request.getTag() + ", " + request.getAmount());
            
            Long userNum = getCurrentUserNum();
            System.out.println("UserNum: " + userNum);
            
            FridgeDto result = fridgeService.updateFood(userNum, fridgeNum, request);
            System.out.println("재료 수정 성공: " + result.getFridgenum());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println("재료 수정 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 재료 삭제
    @DeleteMapping("/delete-food/{fridgeNum}")
    public ResponseEntity<Void> deleteFood(@PathVariable Long fridgeNum) {
        try {
            Long userNum = getCurrentUserNum();
            fridgeService.deleteFood(userNum, fridgeNum);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 태그별 재료 조회
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<FridgeDto>> getFoodsByTag(@PathVariable String tag) {
        try {
            Long userNum = getCurrentUserNum();
            List<FridgeDto> foods = fridgeService.getFoodsByTag(userNum, tag);
            return ResponseEntity.ok(foods);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 음식명으로 검색
    @GetMapping("/search")
    public ResponseEntity<List<FridgeDto>> searchFoodsByName(@RequestParam String foodName) {
        try {
            Long userNum = getCurrentUserNum();
            List<FridgeDto> foods = fridgeService.searchFoodsByName(userNum, foodName);
            return ResponseEntity.ok(foods);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 현재 사용자 번호 가져오기
    private Long getCurrentUserNum() {
        UsernamePasswordAuthenticationToken authentication = 
            (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        
        JwtAuthenticationFilter.JwtUserDetails details = 
            (JwtAuthenticationFilter.JwtUserDetails) authentication.getDetails();
        
        // JwtUserDetails에서 직접 사용자 번호 가져오기
        return details.getUserNum();
    }
}
