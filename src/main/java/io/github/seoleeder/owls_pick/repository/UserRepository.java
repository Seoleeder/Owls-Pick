package io.github.seoleeder.owls_pick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.seoleeder.owls_pick.entity.user.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
