package io.github.seoleeder.owls_pick.dto.auth;

public record SocialUserResponse(
        String providerId, // 고유 식별자 (sub, id 등)
        String email,
        String name       // 닉네임 또는 이름
) {}
