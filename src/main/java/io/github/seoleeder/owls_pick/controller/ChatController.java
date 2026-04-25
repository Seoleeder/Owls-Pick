package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.request.chat.ChatRequest;
import io.github.seoleeder.owls_pick.dto.response.chat.ChatResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.global.security.CustomUserDetails;
import io.github.seoleeder.owls_pick.service.genai.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Owls 챗봇 API", description = "Owls 챗봇 기반 실시간 게임 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "Owls 챗봇 발화 전송 및 AI 응답 수신",
            description = "사용자의 발화(Utterance)와 이전 대화 문맥을 분석하여 RAG 기반 AI 추천 답변을 반환합니다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Authorization 헤더에 JWT 토큰 추가 (예: Bearer eyJhbG...)",
                            in = ParameterIn.HEADER,
                            required = true
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "챗봇 응답 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionId": 12,
                                        "answer": "말씀하신 우주 탐험과 양자 역학 요소를 모두 갖춘 게임으로 'Outer Wilds'를 강력히 추천합니다."
                                      },
                                      "error" : null
                                    }
                                    """)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "인증 실패 (JWT 토큰 누락 또는 만료)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40100,
                                        "message": "인증이 유효하지 않습니다."
                                      }
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "404",
                    description = "리소스 조회 실패 (유저, 채팅 세션, 또는 연관 게임을 찾을 수 없는 경우)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40404,
                                        "message": "존재하지 않는 세션입니다."
                                      }
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 50002,
                                        "message": "FastAPI와의 통신에 실패했습니다."
                                      }
                                    }
                                    """))

            )
    })
    @PostMapping
    public CommonResponse<ChatResponse> processChat(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatService.processRagChat(userDetails.getId(), request);

        return CommonResponse.ok(response);
    }
}
