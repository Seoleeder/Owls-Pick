package io.github.seoleeder.owls_pick.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * SecurityContext의 Principal 자리에 들어갈 커스텀 유저 정보 객체
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;       // DB 상의 User PK
    private final String email;  // 이메일
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String email, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return String.valueOf(id); // 시큐리티 규격상 ID를 문자열로 반환
    }

    @Override
    public String getPassword() {
        return ""; // 소셜 로그인은 비밀번호가 없으므로 공백 처리
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // 나머지 계정 상태 메서드들은 일단 모두 true로 설정합니다.
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
