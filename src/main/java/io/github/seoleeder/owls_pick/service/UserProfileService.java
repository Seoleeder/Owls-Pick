package io.github.seoleeder.owls_pick.service;

import io.github.seoleeder.owls_pick.dto.OnboardingRequest;
import io.github.seoleeder.owls_pick.dto.UserStatusResponse;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.seoleeder.owls_pick.entity.user.User;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    /**
     * 사용자의 온보딩 상태, 성인 여부를 담은 DTO 반환
     * */
    @Transactional(readOnly = true)
    public UserStatusResponse getUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        return UserStatusResponse.from(user);
    }

    /**
     * 사용자의 온보딩 정보(생년월일, 선호 태그/스토어) 업데이트
     */
    @Transactional
    public void completeOnboarding(Long userId, OnboardingRequest request) {
        // 온보딩 대상이 되는 로그인 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 이미 온보딩을 완료한 유저인지 검증
        if (user.isOnboarded()) {
            throw new CustomException(ErrorCode.ALREADY_ONBOARDED);
        }
        // 사용자의 생년월일, 선호 태그 및 스토어 정보 업데이트, 온보딩 완료 상태 변경
        user.completeOnboarding(
                request.birthDate(),
                request.preferredTags(),
                request.preferredStores()
        );

    }
}
