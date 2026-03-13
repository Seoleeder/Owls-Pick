package io.github.seoleeder.owls_pick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.seoleeder.owls_pick.entity.user.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 정보로 유저 조회
    Optional<User> findByEmail(String email);

    // 동일한 닉네임이 존재하는지 검증
    boolean existsByNickname(String nickname);
}
