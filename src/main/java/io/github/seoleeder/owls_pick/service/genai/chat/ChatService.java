package io.github.seoleeder.owls_pick.service.genai.chat;

import io.github.seoleeder.owls_pick.dto.request.chat.*;
import io.github.seoleeder.owls_pick.dto.response.chat.ChatResponse;
import io.github.seoleeder.owls_pick.dto.response.chat.QueryEmbeddingResponse;
import io.github.seoleeder.owls_pick.dto.response.chat.RagGenerationResponse;
import io.github.seoleeder.owls_pick.dto.response.chat.TitleGenerationResponse;
import io.github.seoleeder.owls_pick.entity.game.VectorEmbedding;
import io.github.seoleeder.owls_pick.entity.user.ChatMessage;
import io.github.seoleeder.owls_pick.entity.user.ChatMessage.ChatRole;
import io.github.seoleeder.owls_pick.entity.user.ChatSession;
import io.github.seoleeder.owls_pick.entity.user.User;
import io.github.seoleeder.owls_pick.global.config.properties.GenaiProperties;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.ChatMessageRepository;
import io.github.seoleeder.owls_pick.repository.ChatSessionRepository;
import io.github.seoleeder.owls_pick.repository.UserRepository;
import io.github.seoleeder.owls_pick.repository.VectorEmbeddingRepository;
import io.github.seoleeder.owls_pick.service.genai.chat.event.SessionTitleGenerateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final ChatTrafficService chatTrafficService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VectorEmbeddingRepository vectorEmbeddingRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final RestClient restClient;
    private final ApplicationEventPublisher eventPublisher;
    private final GenaiProperties props;

    private static final int MAX_TITLE_LENGTH = 30; // 세션 타이틀 최대 길이

    public ChatService(
            ChatTrafficService chatTrafficService,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            VectorEmbeddingRepository vectorEmbeddingRepository,
            UserRepository userRepository,
            TransactionTemplate transactionTemplate,
            @Qualifier("chatRestClient") RestClient restClient,
            ApplicationEventPublisher eventPublisher,
            GenaiProperties props) {
        this.chatTrafficService = chatTrafficService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.vectorEmbeddingRepository = vectorEmbeddingRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = transactionTemplate;
        this.restClient = restClient;
        this.eventPublisher = eventPublisher;
        this.props = props;
    }

    /**
     * 트랜잭션 내부에서 로드된 데이터(세션, 대화 내역)를 한 번에 외부로 반환하기 위한 레코드
     */
    private record ChatInitData(Long sessionId, List<ChatHistoryDto> history) {}

    /**
     * RAG 기반 실시간 게임 추천 챗봇 파이프라인
     */
    public ChatResponse processRagChat(Long userId, ChatRequest request) {

        // 락 점유 전 유저 발화문 사전 검증
        if (request.userMessage() == null || request.userMessage().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 유저 단위 동시성 제어 및 트래픽 제한용 분산 락 점유
        chatTrafficService.checkTrafficAndAcquireLock(userId);

        try {
            boolean isNewSession = request.sessionId() == null;

            // 신규 세션 초기 식별용 임시 타이틀 생성
            String initialTitle = null;
            if (isNewSession) {
                initialTitle = request.userMessage().length() > MAX_TITLE_LENGTH
                        ? request.userMessage().substring(0, MAX_TITLE_LENGTH - 3) + "..."
                        : request.userMessage();
            }

            final String finalTitle = initialTitle;

            // 선행 트랜잭션(세션 및 유저 메시지 저장) 분리 실행
            ChatInitData chatInitData = transactionTemplate.execute(status -> {

                // 채팅 세션 및 최근 대화 내역 조회
                ChatSession session = getOrCreateSession(userId, request, finalTitle);
                List<ChatHistoryDto> history = isNewSession
                        ? Collections.emptyList()
                        : getChatHistory(session.getId(), props.chat().historyLimit());

                // 유저 채팅 저장
                saveChatMessage(session.getId(), ChatRole.USER, request.userMessage());

                return new ChatInitData(session.getId(), history);
            });

            if (chatInitData == null) throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);

            Long sessionId = chatInitData.sessionId();
            List<ChatHistoryDto> history = chatInitData.history();

            // 신규 세션 트랜잭션 커밋 완료 후 비동기 타이틀 생성 이벤트 발행
            if (isNewSession) {
                eventPublisher.publishEvent(new SessionTitleGenerateEvent(sessionId, request.userMessage()));
            }

            // 유저 메시지 벡터 임베딩 추출 (사용자 메시지 + 최근 대화 내역)
            float[] queryVector = fetchQueryEmbedding(history, request.userMessage());

            // 벡터 유사도 기반 상위 연관 게임 검색
            List<VectorEmbedding> similarGames = vectorEmbeddingRepository.findTopSimilarGames(queryVector, 5);

            // Vector DB 검색 결과 상세 추적 (데이터 유실 구간 파악)
            if (log.isDebugEnabled()) {
                log.debug("Retrieved {} similar games from Vector DB.", similarGames.size());
                for (int i = 0; i < similarGames.size(); i++) {
                    VectorEmbedding game = similarGames.get(i);
                    String textPreview = game.getSourceText().length() > 50
                            ? game.getSourceText().substring(0, 50) + "..."
                            : game.getSourceText();

                    log.debug("Rank {}: Game ID = {}, Source Text Preview = {}", (i + 1), game.getGameId(), textPreview);
                }
            }

            // 검색된 연관 게임 원본 텍스트 추출 (검색 실패 시 빈 배열 할당으로 일반 응답 유도)
            List<String> contexts = similarGames.isEmpty()
                    ? Collections.emptyList()
                    : similarGames.stream().map(VectorEmbedding::getSourceText).toList();

            // RAG 기반 최종 응답 생성
            String reply = fetchGeneratedChat(history, request.userMessage(), contexts);

            // 생성된 답변 메시지 저장
            transactionTemplate.executeWithoutResult(status ->
                    saveChatMessage(sessionId, ChatRole.ASSISTANT, reply)
            );

            return new ChatResponse(sessionId, reply);
        } finally {
            // 로직 수행 완료 및 예외 발생 여부와 무관하게 점유한 분산 락 반환
            chatTrafficService.releaseLock(userId);
        }
    }

    /**
     * 유효한 채팅 세션 반환 또는 신규 세션 생성
     */
    private ChatSession getOrCreateSession(Long userId, ChatRequest request, String title) {
        if (request.sessionId() != null) {
            ChatSession session = chatSessionRepository.findById(request.sessionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_SESSION));

            // 다른 유저의 세션 접근 방지를 위한 소유권 검증
            if (!session.getUser().getId().equals(userId)) {
                log.warn("[ChatService] Unauthorized session access attempt. UserId: {}, SessionId: {}", userId, request.sessionId());
                throw new CustomException(ErrorCode.NOT_SESSION_OWNER);
            }
            return session;
        }
        // User 프록시 객체 할당
        User userProxy = userRepository.getReferenceById(userId);

        // 신규 세션 저장
        return chatSessionRepository.save(ChatSession.builder()
                .user(userProxy)
                .title(title)
                .build());
    }

    /**
     *  채팅 세션 타이틀 수동 변경
     */
    @Transactional
    public void updateSessionTitle(Long userId, Long sessionId, String newTitle) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_SESSION));

        // 타 유저 세션 접근 방지를 위한 소유권 검증
        if (!session.getUser().getId().equals(userId)) {
            log.warn("[ChatService] Forbidden access attempt. UserId: {}, SessionId: {}", userId, sessionId);
            throw new CustomException(ErrorCode.NOT_SESSION_OWNER);
        }

        session.updateTitle(newTitle);
        log.info("[ChatService] Session title updated. SessionId: {}, NewTitle: {}", sessionId, newTitle);
    }

    /**
     * 최근 대화 내역 조회 및 DTO 변환
     */
    private List<ChatHistoryDto> getChatHistory(Long sessionId, int limit) {
        if (sessionId == null) return Collections.emptyList();

        // 해당 세션의 최근 대화 내역 조회
        List<ChatMessage> messages = chatMessageRepository.findRecentMessages(sessionId, limit);

        // DTO 변환 (시간순 정렬을 위해 ArrayList로 추출)
        List<ChatHistoryDto> dtoList = messages.stream()
                .map(m -> new ChatHistoryDto(
                        m.getChatRole() == ChatRole.USER ? "user" : "model",
                        m.getContent()
                ))
                .collect(Collectors.toCollection(ArrayList::new));

        // 프롬프트 컨텍스트 주입을 위한 시간순(오래된 순) 역순 재배치
        Collections.reverse(dtoList);
        return dtoList;
    }

    /**
     * 단일 채팅 메시지 저장
     */
    private void saveChatMessage(Long sessionId, ChatRole role, String content) {
        // 세션 프록시 객체 할당
        ChatSession sessionProxy = chatSessionRepository.getReferenceById(sessionId);

        chatMessageRepository.save(ChatMessage.builder()
                .chatSession(sessionProxy)
                .chatRole(role)
                .content(content)
                .build());
    }

    /**
     * FastAPI 서버에 메시지 임베딩 요청 및 응답 반환
     */
    private float[] fetchQueryEmbedding(List<ChatHistoryDto> history, String userMessage) {
        URI targetUri = UriComponentsBuilder.fromUriString(props.fastapiUrl())
                .path("/api/genai/chat/embeddings/query")
                .build()
                .toUri();

        try {
            QueryEmbeddingResponse response = restClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new QueryEmbeddingRequest(history, userMessage))
                    .retrieve()
                    .body(QueryEmbeddingResponse.class);

            if (response == null || response.vector() == null) {
                log.error("[ChatService] Invalid response from FastAPI embedding endpoint.");
                throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
            }
            return response.vector();

        } catch (RestClientException e) {
            log.error("[ChatService] Failed to communicate with FastAPI for query embedding.", e);
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        }
    }
    /**
     * FastAPI 서버에 RAG 프롬프트 기반 텍스트 생성 요청
     */
    private String fetchGeneratedChat(List<ChatHistoryDto> history, String userMessage, List<String> contexts) {
        URI targetUri = UriComponentsBuilder.fromUriString(props.fastapiUrl())
                .path("/api/genai/chat/generate")
                .build()
                .toUri();

        try {
            RagGenerationResponse response = restClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RagGenerationRequest(history, userMessage, contexts))
                    .retrieve()
                    .body(RagGenerationResponse.class);

            if (response == null || response.reply() == null) {
                log.error("[ChatService] Invalid response from FastAPI generation endpoint.");
                throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
            }
            return response.reply();

        } catch (RestClientException e) {
            log.error("[ChatService] Failed to communicate with FastAPI for RAG chat generation.", e);
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        }
    }
}