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

}
