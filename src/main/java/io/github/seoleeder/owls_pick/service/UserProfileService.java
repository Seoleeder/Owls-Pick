package io.github.seoleeder.owls_pick.service;

import io.github.seoleeder.owls_pick.dto.request.MyPageUpdateRequest;
import io.github.seoleeder.owls_pick.dto.request.OnboardingRequest;
import io.github.seoleeder.owls_pick.dto.response.MyPageResponse;
import io.github.seoleeder.owls_pick.dto.response.UserStatusResponse;
import io.github.seoleeder.owls_pick.entity.user.SocialAccount;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.SocialAccountRepository;
import io.github.seoleeder.owls_pick.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.seoleeder.owls_pick.entity.user.User;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    // ------ 온보딩 메서드 ------

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

        // 닉네임 중복 검증
        if (!isNicknameAvailable(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME); // 예: 409 Conflict (에러 코드 추가 필요)
        }

        // 사용자의 생년월일, 선호 태그 및 스토어 정보 업데이트, 온보딩 완료 상태 변경
        user.completeOnboarding(
                request.nickname(),
                request.birthDate(),
                request.preferredTags(),
                request.preferredStores()
        );
    }

    // ------ 마이페이지 메서드 ------

    /**
     * 마이 페이지 통합 정보 조회 (닉네임, 이메일, 선호 태그 및 스토어 등)
     */
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId) {

        // 유저 기본 정보 데이터 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 소셜 제공자 데이터 조회
        SocialAccount socialAccount = socialAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 마이페이지에 노출될 모든 데이터 반환 (위시리스트 제외)
        return MyPageResponse.builder()
                .provider(socialAccount.getProvider().name())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .isDiscountNotificationEnabled(user.isDiscountNotificationEnabled())
                .preferredTags(user.getPreferredTags())
                .preferredStores(user.getPreferredStores())
                .build();
    }

    /**
     * 마이 페이지 정보 부분 수정 (PATCH)
     */
    @Transactional
    public void updateMyPage(Long userId, MyPageUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 닉네임 변경 요청
        if (request.nickname() != null) {
            // 기존 닉네임과 다를 경우에만 중복 검사 실행
            if (!user.getNickname().equals(request.nickname()) && !isNicknameAvailable(request.nickname())) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
            }
            user.updateNickname(request.nickname());
        }

        // 위시리스트 알림 설정 변경 요청
        if (request.isDiscountNotificationEnabled() != null) {
            user.updateDiscountNotification(request.isDiscountNotificationEnabled());
        }

        // 선호 태그 변경 요청
        if (request.preferredTags() != null) {
            user.updatePreferredTags(request.preferredTags());
        }

        // 선호 스토어 변경 요청
        if (request.preferredStores() != null) {
            user.updatePreferredStores(request.preferredStores());
        }
    }

    /**
     * 닉네임 중복 확인 (온보딩 및 마이페이지에서 공통 사용)
     */
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }
}
