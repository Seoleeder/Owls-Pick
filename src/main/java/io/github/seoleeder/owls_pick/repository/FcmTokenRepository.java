package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.notification.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // 특정 토큰 문자열 조회
    Optional<FcmToken> findByToken(String token);

    // 특정 유저의 모든 토큰 조회
    List<FcmToken> findAllByUserId(Long userId);

    // 유효하지 않은 토큰들을 한꺼번에 삭제
    void deleteByTokenIn(List<String> tokens);

    // 특정 사용자의 모든 기기 토큰 삭제
    void deleteAllByUserId(Long userId);
}
