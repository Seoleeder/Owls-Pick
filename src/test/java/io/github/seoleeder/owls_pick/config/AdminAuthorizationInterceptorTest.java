package io.github.seoleeder.owls_pick.config;

import io.github.seoleeder.owls_pick.common.config.AdminAuthorizationInterceptor;
import io.github.seoleeder.owls_pick.common.config.WebConfig;
import io.github.seoleeder.owls_pick.common.config.properties.AdminProperties;
import io.github.seoleeder.owls_pick.common.response.ErrorCode;
import io.github.seoleeder.owls_pick.common.response.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//테스트용 더미 컨트롤러
@RestController
class DummyController {
    @GetMapping("/admin/test")
    public String test() { return "ok"; }
}

@WebMvcTest(DummyController.class) // 더미 컨트롤러만 띄움
@AutoConfigureMockMvc(addFilters = false) //security Filter 무력화
@Import({WebConfig.class, AdminAuthorizationInterceptor.class, GlobalExceptionHandler.class})
@EnableConfigurationProperties(AdminProperties.class)   // 인터셉터 관련 설정 로드
@TestPropertySource(properties = "owls-pick.admin-key=valid-key")
class AdminAuthorizationInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("[성공] 올바른 키를 보내면 인터셉터 통과")
    void preHandle_Success() throws Exception {
        mockMvc.perform(get("/admin/test")
                        .header("X-ADMIN-KEY", "valid-key"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[실패] 잘못된 키를 보내면 ADMIN_KEY_ERROR 예외 발생")
    void preHandle_Fail_WrongKey() throws Exception {
        mockMvc.perform(get("/admin/test")
                        .header("X-ADMIN-KEY", "wrong-key"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ADMIN_KEY_ERROR.getCode()));
    }

    @Test
    @DisplayName("[실패] 헤더가 없으면 예외 발생")
    void preHandle_Fail_NoHeader() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isUnauthorized());
    }
}