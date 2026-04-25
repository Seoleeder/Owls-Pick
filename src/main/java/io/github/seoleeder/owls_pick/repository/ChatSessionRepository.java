package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.user.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
}
