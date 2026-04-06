package io.github.seoleeder.owls_pick.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "GenAI 리뷰 요약 응답 DTO (FastAPI 통신용)")
public record ReviewSummaryResponse(

        @Schema(description = " 스팀 리뷰 요약 텍스트", example = "대체로 타격감이 훌륭하고 스토리 몰입도가 높다는 평입니다.")
        String summaryText,

        @Schema(description = "가장 많이 언급된 긍정 키워드 5개", example = "[\"타격감\", \"스토리\", \"그래픽\", \"사운드\", \"연출\"]")
        List<String> positiveKeywords,

        @Schema(description = "가장 많이 언급된 부정 키워드 5개", example = "[\"버그\", \"최적화\", \"서버불안정\", \"번역\", \"프레임드랍\"]")
        List<String> negativeKeywords
) {
}
