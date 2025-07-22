package com.fridgemarket.fridgemarket.service.social; // 이 클래스가 속한 패키지를 선언합니다. 소셜 로그인 관련 서비스들을 관리하는 패키지입니다.

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.UserRepository;
import com.fridgemarket.fridgemarket.helper.constants.SocialLoginType;
import lombok.RequiredArgsConstructor; // Lombok 라이브러리에서 @RequiredArgsConstructor 어노테이션을 임포트합니다. 이 어노테이션은 final 필드나 @NonNull 필드에 대한 생성자를 자동으로 생성해 줍니다.
import org.springframework.beans.factory.annotation.Value; // 스프링의 @Value 어노테이션을 임포트합니다. 이 어노테이션은 설정 파일(예: application.properties, application.yml)의 값을 주입받을 때 사용됩니다.
import org.springframework.http.HttpStatus; // HTTP 상태 코드(예: 200 OK, 404 Not Found)를 나타내는 HttpStatus enum을 임포트합니다.
import org.springframework.http.ResponseEntity; // HTTP 응답 전체(상태 코드, 헤더, 본문)를 캡슐화하는 ResponseEntity 클래스를 임포트합니다.
import org.springframework.stereotype.Component; // 이 클래스를 스프링의 컴포넌트로 등록하기 위한 @Component 어노테이션을 임포트합니다.
import org.springframework.web.client.RestTemplate; // RESTful API 호출을 위한 스프링의 RestTemplate 클래스를 임포트합니다.

import java.util.HashMap; // 키-값 쌍을 저장하는 HashMap 클래스를 임포트합니다.
import java.util.Map; // Map 인터페이스를 임포트합니다.
import java.util.stream.Collectors; // Java Stream API에서 요소들을 수집하는 Collectors 유틸리티 클래스를 임포트합니다.

@Component // 이 클래스를 스프링 컨테이너에 의해 관리되는 컴포넌트로 등록합니다. 스프링이 이 클래스를 스캔하여 빈(Bean)으로 만듭니다.
@RequiredArgsConstructor // final로 선언된 필드들(여기서는 @Value로 주입될 필드들)을 매개변수로 받는 생성자를 자동으로 생성해 줍니다.
public class GoogleOauth implements SocialOauth { // GoogleOauth 클래스를 선언하고, SocialOauth 인터페이스를 구현합니다. 이는 구글 OAuth2 인증을 처리하는 로직을 정의합니다.

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${sns.google.url}") // application.properties 또는 application.yml 파일에서 'sns.google.url' 키에 해당하는 값을 이 필드에 주입합니다. 구글 인증 서버의 기본 URL입니다.
    private String GOOGLE_SNS_BASE_URL; // 구글 소셜 로그인 요청을 시작할 기본 URL (예: https://accounts.google.com/o/oauth2/v2/auth)

    @Value("${sns.google.client.id}") // 설정 파일에서 'sns.google.client.id' 키에 해당하는 값을 주입합니다. 구글 API 콘솔에서 발급받은 클라이언트 ID입니다.
    private String GOOGLE_SNS_CLIENT_ID; // 구글 클라이언트 ID

    @Value("${sns.google.callback.url}") // 설정 파일에서 'sns.google.callback.url' 키에 해당하는 값을 주입합니다. 구글 인증 후 리다이렉트될 애플리케이션의 URL입니다.
    private String GOOGLE_SNS_CALLBACK_URL; // 구글 인증 후 돌아올 콜백 URL

    @Value("${sns.google.client.secret}") // 설정 파일에서 'sns.google.client.secret' 키에 해당하는 값을 주입합니다. 구글 API 콘솔에서 발급받은 클라이언트 시크릿입니다.
    private String GOOGLE_SNS_CLIENT_SECRET; // 구글 클라이언트 시크릿

    @Value("${sns.google.token.url}") // 설정 파일에서 'sns.google.token.url' 키에 해당하는 값을 주입합니다. 액세스 토큰을 요청할 구글 인증 서버의 URL입니다.
    private String GOOGLE_SNS_TOKEN_BASE_URL; // 구글 액세스 토큰을 요청할 URL (예: https://oauth2.googleapis.com/token)

    @Override // 부모 인터페이스(SocialOauth)의 메서드를 오버라이드(재정의)함을 나타냅니다.
    // ⭐ 수정: SocialOauth 인터페이스와 일치하도록 getOauthRedirectUrl()로 변경 ⭐ // 주석: 인터페이스 메서드 이름 변경에 대한 가이드라인입니다.
    public String getOauthRedirectUrl() { // OAuth2 인증을 시작하기 위한 구글 로그인 페이지의 리다이렉트 URL을 생성하여 반환합니다.
        Map<String, Object> params = new HashMap<>(); // URL 쿼리 파라미터를 담을 HashMap 객체를 생성합니다.
        params.put("scope", "profile email"); // "scope" 파라미터에 "profile"과 "email"을 설정합니다. 이는 구글로부터 사용자의 프로필 정보와 이메일 주소를 요청함을 의미합니다.
        params.put("response_type", "code"); // "response_type"을 "code"로 설정합니다. 이는 인증 코드(authorization code) 방식을 사용하여 토큰을 요청할 것임을 의미합니다.
        params.put("client_id", GOOGLE_SNS_CLIENT_ID); // "client_id" 파라미터에 위에서 주입받은 구글 클라이언트 ID를 설정합니다.
        params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL); // "redirect_uri" 파라미터에 구글 인증 후 리다이렉트될 콜백 URL을 설정합니다.
        params.put("provider", "google");

        String parameterString = params.entrySet().stream() // Map의 모든 엔트리(키-값 쌍)를 스트림으로 변환합니다.
                .map(x -> x.getKey() + "=" + x.getValue()) // 각 엔트리를 "키=값" 형태의 문자열로 변환합니다. (예: "scope=profile email")
                .collect(Collectors.joining("&")); // 변환된 문자열들을 '&' 문자로 연결하여 하나의 쿼리 스트링을 만듭니다. (예: "scope=profile email&response_type=code&...")

        return GOOGLE_SNS_BASE_URL + "?" + parameterString; // 구글 인증 서버의 기본 URL에 생성된 쿼리 스트링을 붙여 완전한 리다이렉트 URL을 반환합니다.
    }

    @Override // 부모 인터페이스(SocialOauth)의 메서드를 오버라이드함을 나타냅니다.
    public String requestAccessToken(String code) { // 구글로부터 받은 인증 코드(code)를 사용하여 액세스 토큰을 요청하고 반환하는 메서드입니다.
        RestTemplate restTemplate = new RestTemplate(); // RESTful API 호출을 수행할 RestTemplate 객체를 생성합니다.

        Map<String, Object> params = new HashMap<>(); // 액세스 토큰 요청에 필요한 파라미터들을 담을 HashMap 객체를 생성합니다.
        params.put("code", code); // 구글로부터 전달받은 인증 코드입니다.
        params.put("client_id", GOOGLE_SNS_CLIENT_ID); // 구글 클라이언트 ID입니다.
        params.put("client_secret", GOOGLE_SNS_CLIENT_SECRET); // 구글 클라이언트 시크릿입니다.
        params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL); // 인증 코드를 받을 때 사용했던 콜백 URL과 동일해야 합니다.
        params.put("grant_type", "authorization_code"); // 인증 코드 방식으로 액세스 토큰을 요청함을 명시합니다.

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(GOOGLE_SNS_TOKEN_BASE_URL, params, String.class);
        // GOOGLE_SNS_TOKEN_BASE_URL(액세스 토큰 요청 URL)로 POST 요청을 보냅니다.
        // params는 요청 본문에 포함될 데이터입니다.
        // String.class는 응답 본문의 타입을 String으로 받겠다는 의미입니다.
        // 요청 결과는 ResponseEntity<String> 객체로 반환됩니다.

        if (responseEntity.getStatusCode() == HttpStatus.OK) { // HTTP 응답 상태 코드가 200 OK인지 확인합니다.
            return responseEntity.getBody(); // 응답 상태가 OK이면, 응답 본문(액세스 토큰 정보가 포함된 JSON 문자열)을 반환합니다.
        }
        // 오류 처리 개선: 예외를 던지거나 더 자세한 오류 정보를 반환하는 것이 좋습니다. // 주석: 오류 처리 로직 개선에 대한 제안입니다.
        throw new RuntimeException("구글 액세스 토큰 요청 실패: " + responseEntity.getStatusCode() + " - " + responseEntity.getBody());
        // 응답 상태가 OK가 아니면, RuntimeException을 발생시켜 액세스 토큰 요청이 실패했음을 알립니다.
        // 예외 메시지에는 HTTP 상태 코드와 응답 본문이 포함되어 디버깅에 도움을 줍니다.
    }

    @Override
    public void processUser(String code) {
        String accessToken = requestAccessToken(code);
        RestTemplate restTemplate = new RestTemplate();
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        // 사용자 정보 요청
        ResponseEntity<String> responseEntity = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            try {
                Map<String, Object> userInfo = objectMapper.readValue(responseEntity.getBody(), Map.class);
                String providerId = (String) userInfo.get("id");
                String nickname = (String) userInfo.get("name");
                String email = (String) userInfo.get("email");

                User user = userRepository.findByProviderAndUserid("google", providerId).orElseGet(() -> {
                    User newUser = new User();
                    newUser.setProvider("google");
                    newUser.setUserid(providerId);
                    return newUser;
                });

                user.setNickname(nickname);
                user.setEmail(email);
                userRepository.save(user);
            } catch (Exception e) {
                throw new RuntimeException("사용자 정보 처리 실패");
            }
        } else {
            throw new RuntimeException("사용자 정보 요청 실패");
        }
    }

    @Override
    public SocialLoginType type() {
        return SocialLoginType.GOOGLE;
    }
}
