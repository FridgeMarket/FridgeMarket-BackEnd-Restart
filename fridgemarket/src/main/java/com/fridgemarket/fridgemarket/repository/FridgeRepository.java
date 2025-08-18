package com.fridgemarket.fridgemarket.repository;

import com.fridgemarket.fridgemarket.DAO.Fridge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FridgeRepository extends JpaRepository<Fridge, Long> {
    // 사용자별 냉장고 재료 조회
    List<Fridge> findByUser_Usernum(Long userNum);
    
    // 사용자별 특정 태그의 재료 조회
    List<Fridge> findByUser_UsernumAndTag(Long userNum, String tag);
    
    // 사용자별 특정 음식명으로 조회
    List<Fridge> findByUser_UsernumAndFoodnameContaining(Long userNum, String foodName);
}

