package com.fridgemarket.fridgemarket.controller;

import com.fridgemarket.fridgemarket.DAO.Chat;
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.config.JwtAuthenticationFilter;
import com.fridgemarket.fridgemarket.dto.ChatDto;
import com.fridgemarket.fridgemarket.service.ChatService;
import com.fridgemarket.fridgemarket.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 쪽지(채팅) 기능 컨트롤러
 * 요구사항 매핑
 * - 게시물 상세에서 "쪽지 보내기" 버튼 → 발신자가 수신자(게시물 작성자)에게 메시지 전송
 * - 프로필의 "쪽지함" → 받은 쪽지함/보낸 쪽지함 분리 조회
 * - 받은 쪽지 클릭 시 닉네임/보낸 시간/내용 조회 + 답장 가능
 */
@RestController
@RequestMapping("")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails jwtDetails) {
            return userService.findByProviderAndSocialId(jwtDetails.getProvider(), jwtDetails.getSocialId()).orElse(null);
        }
        return null;
    }

    /**
     * 쪽지 보내기
     * - body: { receiverNickname: String, content: String }
     * - sender는 현재 로그인 사용자
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatDto> sendMessage(@RequestBody SendRequest request) {
        User sender = getCurrentUser();
        if (sender == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        if (request == null || request.receiverNickname == null || request.receiverNickname.isBlank() || request.content == null || request.content.isBlank()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 닉네임으로 수신자 조회
        User receiver = userService.findByNickname(request.receiverNickname);
        if (receiver == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Chat saved = chatService.sendMessage(sender, receiver, request.content);
        return new ResponseEntity<>(toDto(saved), HttpStatus.CREATED);
    }

    /**
     * 받은 쪽지함
     */
    @GetMapping("/receive-chat")
    public ResponseEntity<List<ChatDto>> inbox() {
        User user = getCurrentUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<ChatDto> dtos = chatService.getInbox(user).stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 보낸 쪽지함
     */
    @GetMapping("/send-chat")
    public ResponseEntity<List<ChatDto>> sent() {
        User user = getCurrentUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<ChatDto> dtos = chatService.getSent(user).stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 쪽지 상세 조회 (권한: 발신자 또는 수신자)
     * - 경로 변수 chat_num 로 특정 쪽지를 조회합니다.
     */
    @GetMapping("/check-chat/{chat_num}")
    public ResponseEntity<ChatDto> getMessage(@PathVariable("chat_num") Long chatNum) {
        User user = getCurrentUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Chat chat = chatService.getMessage(chatNum);
        if (chat == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if (!chat.getSender().getUsernum().equals(user.getUsernum()) && !chat.getReceiver().getUsernum().equals(user.getUsernum())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(toDto(chat));
    }

    /**
     * 답장 보내기: 기존 메시지의 수신자 ↔ 발신자를 바꿔서 새 메시지 전송
     */
    @PostMapping("/reply/{chat_num}")
    public ResponseEntity<ChatDto> reply(@PathVariable("chat_num") Long chatNum, @RequestBody ReplyRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Chat origin = chatService.getMessage(chatNum);
        if (origin == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if (!origin.getSender().getUsernum().equals(user.getUsernum()) && !origin.getReceiver().getUsernum().equals(user.getUsernum())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        if (request == null || request.content == null || request.content.isBlank()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        User receiver = origin.getSender().getUsernum().equals(user.getUsernum()) ? origin.getReceiver() : origin.getSender();
        Chat saved = chatService.sendMessage(user, receiver, request.content);
        return new ResponseEntity<>(toDto(saved), HttpStatus.CREATED);
    }

    /**
     * 쪽지 삭제(소프트 삭제)
     */
    @DeleteMapping("/delete-chat/{chat_num}")
    public ResponseEntity<Void> delete(@PathVariable("chat_num") Long chatNum) {
        User user = getCurrentUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        chatService.deleteMessage(user, chatNum);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ========= DTO/요청 변환 유틸 =========
    private ChatDto toDto(Chat chat) {
        ChatDto dto = ChatDto.toDto(chat);
        // 보낸 시간 문자열이 필요하면 확장 가능 (현재 ChatDto에는 필드 없음)
        return dto;
    }

    public static class SendRequest {
        public String receiverNickname; // 수신자 닉네임
        public String content;          // 메시지 내용
    }

    public static class ReplyRequest {
        public String content;          // 답장 내용
    }
}


