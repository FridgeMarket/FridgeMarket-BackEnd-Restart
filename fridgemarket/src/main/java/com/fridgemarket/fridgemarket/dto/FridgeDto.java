package com.fridgemarket.fridgemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FridgeDto {
    private Long fridgenum;
    private String foodname;
    private String tag;
    private Long amount;
    private Date registeredat;
    
    // 생성용 DTO (ID 제외)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String foodname;
        private String tag;
        private Long amount;
        
        // Getter, Setter
        public String getFoodname() { return foodname; }
        public void setFoodname(String foodname) { this.foodname = foodname; }
        
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
    }
    
    // 수정용 DTO
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String foodname;
        private String tag;
        private Long amount;
        
        // Getter, Setter
        public String getFoodname() { return foodname; }
        public void setFoodname(String foodname) { this.foodname = foodname; }
        
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
    }
}
