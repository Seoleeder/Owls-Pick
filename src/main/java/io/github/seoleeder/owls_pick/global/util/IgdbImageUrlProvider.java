package io.github.seoleeder.owls_pick.global.util;

import io.github.seoleeder.owls_pick.global.config.properties.IgdbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IgdbImageUrlProvider {

    private final IgdbProperties props;

    /**
     * 전역으로 사용되는 Igdb Image URL 생성 로직
     */
    public String generateImageUrl(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return null; // id 없으면 기본 이미지 넣도록 유도
        }
        return String.format("https://%s/%s.jpg", props.imageUrl(), imageId);
    }

    /**
     * 특정 사이즈(size)를 지정하여 IGDB Image URL 생성
     * 상세 페이지(t_1080p)나 스크린샷(t_screenshot_huge) 등에 사용합니다.
     */
    public String generateImageUrl(String imageId, String size) {
        if (imageId == null || imageId.isBlank()) {
            return null; // id 없으면 프론트에서 기본 이미지 처리하도록 null 반환
        }

        // 주의: props.imageUrl()이 "images.igdb.com/igdb/image/upload" 형태라고 가정합니다.
        // IGDB 공식 포맷: https://images.igdb.com/igdb/image/upload/t_{size}/{image_id}.jpg
        return String.format("https://%s/t_%s/%s.jpg", props.imageUrl(), size, imageId);
    }

}
