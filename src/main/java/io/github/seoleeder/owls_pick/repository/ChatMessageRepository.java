package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.user.ChatMessage;
import io.github.seoleeder.owls_pick.repository.Custom.ChatMessageRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {
}
