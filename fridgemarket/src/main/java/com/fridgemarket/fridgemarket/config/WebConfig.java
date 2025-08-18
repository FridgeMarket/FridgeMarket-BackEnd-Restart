package com.fridgemarket.fridgemarket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 설정 클래스
 * 
 * 주요 기능:
 * 1. 정적 리소스 핸들러 설정
 * 2. 업로드된 파일 접근 경로 설정
 * 
 * 설정 내용:
 * - "/uploads/**" URL 패턴을 "file:uploads/" 디렉토리와 매핑
 * - 사용자가 업로드한 이미지나 파일들을 웹에서 접근 가능하게 함
 * - 프로필 이미지, 게시글 이미지 등 파일 서빙
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들러를 추가하는 메서드
     * 
     * 설정 내용:
     * - "/uploads/**" 패턴의 URL 요청을 처리하는 리소스 핸들러 등록
     * - 실제 파일이 저장된 "file:uploads/" 디렉토리와 매핑
     * - 업로드된 이미지나 파일들을 웹에서 접근 가능하게 함
     * 
     * 예시:
     * - URL: http://localhost:8080/uploads/profile_images/abc123.jpg
     * - 실제 파일: uploads/profile_images/abc123.jpg
     * 
     * @param registry 리소스 핸들러 레지스트리
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ========== 업로드 파일 접근 설정 ==========
        registry.addResourceHandler("/uploads/**") // 웹에서 접근할 URL 패턴
                .addResourceLocations("file:uploads/"); // 실제 파일이 저장된 디렉토리 경로
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // ========== CORS 설정 (iOS 앱 연동용) ==========
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 모든 도메인 허용 (개발용)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 1시간
    }
}
