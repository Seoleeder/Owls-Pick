package io.github.seoleeder.owls_pick.controller.auth;

import io.github.seoleeder.owls_pick.service.auth.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhook")
@Tag(name = "웹훅 API", description = "외부 시스템(카카오, 네이버 등)에서 서버로 보내는 알림을 처리합니다.")
public class WebhookController {

    private final WebhookService webhookService;

    @Operation(summary = "카카오 연동 해제 웹훅",
            description = "사용자가 카카오 설정에서 앱 연동을 해제하면 회원 탈퇴 처리")
    @PostMapping(value = "/kakao/unlink", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> kakaoUnlink(@RequestHeader("Authorization") String auth, @RequestParam("user_id") String pid) {
        webhookService.handleKakaoUnlink(auth, pid);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "네이버 연동 해제 웹훅",
            description = "사용자가 네이버 설정에서 서비스 연결을 끊으면 회원 탈퇴 처리")
    @PostMapping(value = "/naver/unlink", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> naverUnlink(@RequestParam String clientId, @RequestParam String encryptUniqueId,
                                            @RequestParam String timestamp, @RequestParam String signature) {
        webhookService.handleNaverUnlink(clientId, encryptUniqueId, timestamp, signature);
        return ResponseEntity.noContent().build(); // 204
    }
}