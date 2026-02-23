package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.user.SocialAccount;
import io.github.seoleeder.owls_pick.entity.user.SocialAccount.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(Provider provider, String providerId);
    void deleteByUserId(Long userId);
}
