package com.fridgemarket.fridgemarket.dto;

import com.fridgemarket.fridgemarket.DAO.Chat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // getter, setter 등 메서드를 자동 생성해서 편리함 제공
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto {
    private String chatnum;
    private String content;
    private String sender;
    private String receiver;
    private String sendTime; // 보낸 시간 (문자열로 포맷팅해 전달)

    public static ChatDto toDto(Chat chat) {
        String formatted = chat.getSendtime() != null ? chat.getSendtime().toString() : null;
        return new ChatDto(
                String.valueOf(chat.getChatnum()),
                chat.getContent(),
                chat.getSender().getNickname(),
                chat.getReceiver().getNickname(),
                formatted
        );
    }
}
