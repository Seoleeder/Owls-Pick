package io.github.seoleeder.owls_pick.repository.Custom;

import io.github.seoleeder.owls_pick.entity.user.ChatMessage;

import java.util.List;

public interface ChatMessageRepositoryCustom {
    // 세션의 최근 대화 내역을 지정한 개수만큼 최신순으로 조회
    List<ChatMessage> findRecentMessages(Long sessionId, int limit);
}
