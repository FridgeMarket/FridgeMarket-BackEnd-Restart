package com.fridgemarket.fridgemarket.service;

import com.fridgemarket.fridgemarket.DAO.Chat;
import com.fridgemarket.fridgemarket.DAO.User;
import com.fridgemarket.fridgemarket.repository.ChatRepository;
import com.fridgemarket.fridgemarket.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final AppUserRepository userRepository;

    public ChatService(ChatRepository chatRepository, AppUserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    /**
     * 쪽지 보내기
     * - sender(보낸 사람), receiver(받는 사람), content를 받아 메시지를 저장
     * - 양쪽 삭제 플래그 초기값은 false, 보낸 시간은 현재 시간
     */
    @Transactional
    public Chat sendMessage(User sender, User receiver, String content) {
        Chat chat = new Chat();
        chat.setSender(sender);
        chat.setReceiver(receiver);
        chat.setContent(content);
        chat.setSendtime(LocalDateTime.now());
        chat.setDeletedBySender(false);
        chat.setDeletedByReceiver(false);
        return chatRepository.save(chat);
    }

    /**
     * 받은 쪽지함 조회 (수신자가 삭제하지 않은 메시지)
     */
    public List<Chat> getInbox(User receiver) {
        return chatRepository.findByReceiverAndDeletedByReceiverFalseOrderBySendtimeDesc(receiver);
    }

    /**
     * 보낸 쪽지함 조회 (발신자가 삭제하지 않은 메시지)
     */
    public List<Chat> getSent(User sender) {
        return chatRepository.findBySenderAndDeletedBySenderFalseOrderBySendtimeDesc(sender);
    }

    /**
     * 단일 쪽지 조회 (권한 검증은 컨트롤러/상위 레이어에서 수행)
     */
    public Chat getMessage(Long chatId) {
        return chatRepository.findById(chatId).orElse(null);
    }

    /**
     * 메시지 삭제(소프트 삭제)
     * - 요청자가 발신자면 deletedBySender=true, 수신자면 deletedByReceiver=true
     * - 양쪽 모두 true면 실제 삭제는 후속 배치 혹은 별도 정리 로직에서 처리 가능
     */
    @Transactional
    public void deleteMessage(User requester, Long chatId) {
        Chat chat = chatRepository.findById(chatId).orElse(null);
        if (chat == null) return;

        if (chat.getSender() != null && chat.getSender().getUsernum().equals(requester.getUsernum())) {
            chat.setDeletedBySender(true);
        }
        if (chat.getReceiver() != null && chat.getReceiver().getUsernum().equals(requester.getUsernum())) {
            chat.setDeletedByReceiver(true);
        }

        if (chat.isDeleted()) {
            chatRepository.delete(chat);
        } else {
            chatRepository.save(chat);
        }
    }
}


