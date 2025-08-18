package com.fridgemarket.fridgemarket.DAO;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Chat {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_num") // 쪽지 번호
    private Long chatnum;

    @ManyToOne(fetch = FetchType.LAZY) // 사용자 1명 기준 편지를 여러개 보낼 수 있음 , 지연 로딩 (메시지만 조회)
    @JoinColumn(name = "senderNickname") // 쪽지를 보내는 사람
    @OnDelete(action = OnDeleteAction.NO_ACTION) // 작성자 혹은 수신자가 계정을 삭제하면, 같이 지우기 위해서
    private User sender;

    @ManyToOne (fetch = FetchType.LAZY)// 사용자 1명 기준 편지를 여러개 받을 수 있음 , 지연 로딩 (메시지만 조회)
    @JoinColumn(name = "receiverNickname")
    @OnDelete(action = OnDeleteAction.NO_ACTION) // 작성자 혹은 수신자가 계정을 삭제하면, 같이 지우기 위해서
    private User receiver; // 쪽지를 받는 사람

    @Column(name = "content") // 메시지 내용
    private String content;

    @Column(name = "send_time") // 보낸 시간
    private LocalDateTime sendtime;

    @Column(nullable = false)
    private boolean deletedBySender;

    @Column(nullable = false)
    private boolean deletedByReceiver;

    public void deletedBySender() { // 발신자가 메세지를 삭제하면 true로 변경
        this.deletedBySender = true;
    }

    public void deletedByReceiver() { // 수신자가 메시지를 삭제하면 true로 변경
        this.deletedByReceiver = true;
    }

    public boolean isDeleted() { // 둘 다 true일 경우 DB에서 물리적으로 삭제 처리 가능
        return isDeletedBySender() && isDeletedByReceiver();
    }
}