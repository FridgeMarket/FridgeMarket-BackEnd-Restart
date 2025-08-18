package com.fridgemarket.fridgemarket.repository;

import com.fridgemarket.fridgemarket.DAO.Chat;
import com.fridgemarket.fridgemarket.DAO.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    // 받은 쪽지함: 수신자 기준, 수신자가 삭제하지 않은 메시지만, 최신순
    List<Chat> findByReceiverAndDeletedByReceiverFalseOrderBySendtimeDesc(User receiver);

    // 보낸 쪽지함: 발신자 기준, 발신자가 삭제하지 않은 메시지만, 최신순
    List<Chat> findBySenderAndDeletedBySenderFalseOrderBySendtimeDesc(User sender);
}


