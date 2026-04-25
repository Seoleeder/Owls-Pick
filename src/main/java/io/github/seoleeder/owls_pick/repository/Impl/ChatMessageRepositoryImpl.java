package io.github.seoleeder.owls_pick.repository.Impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.entity.user.ChatMessage;
import io.github.seoleeder.owls_pick.repository.Custom.ChatMessageRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static io.github.seoleeder.owls_pick.entity.user.QChatMessage.chatMessage;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 특정 세션에서의 최근 대화 내역을 지정한 개수만큼 최신순으로 조회
     * */
    @Override
    public List<ChatMessage> findRecentMessages(Long sessionId, int limit) {
        return queryFactory.selectFrom(chatMessage)
                .where(chatMessage.chatSession.id.eq(sessionId))
                .orderBy(chatMessage.createdAt.desc()) // 최신순 정렬
                .limit(limit)                          // 동적 Limit 적용
                .fetch();
    }
}
