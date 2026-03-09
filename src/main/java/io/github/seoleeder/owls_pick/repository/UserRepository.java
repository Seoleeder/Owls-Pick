package io.github.seoleeder.owls_pick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.seoleeder.owls_pick.entity.user.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
