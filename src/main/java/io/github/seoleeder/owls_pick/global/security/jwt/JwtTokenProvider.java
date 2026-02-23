package io.github.seoleeder.owls_pick.global.security.jwt;

import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.jsonwebtoken.*;

import io.jsonwebtoken.security.SecurityException;
import io.github.seoleeder.owls_pick.global.security.config.properties.JwtProperties;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    /**
     * 객체 생성 후 Base64로 인코딩된 secret 값을 디코딩하여 Key 객체로 변환
     * HMAC-SHA 알고리즘 사용
     * */
    @PostConstruct
    public void init(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     * 사용자의 권한 정보 포함
     * 만료 시간 : 2주
     * */
    public String createAccessToken(Authentication authentication){
        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity = new Date(now + jwtProperties.expiration());

        return Jwts.builder()
                .subject(authentication.getName())      // 사용자 식별자 (ID)
                .claim("auth", authorities)             // 핵심: 권한 정보를 토큰에 포함
                .issuedAt(new Date())
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Refresh Token 생성
     * */
    public String createRefreshToken(String userId){
        long now = (new Date()).getTime();
        Date validity = new Date(now + jwtProperties.refreshTokenValidity());

        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * JWT에서 인증 정보 (Authentication) 추출
     * */
    public Authentication getAuthentication(String accessToken){
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            log.error("토큰에 권한(auth) 정보가 없습니다");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // Claim에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 반환
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /**
     * 발급된 토큰의 유효성 검증
     * */
    public void validateToken(String token) {
        Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * JWT에서 토큰의 내용을 담은 Claim 추출
     * */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * 토큰에서 유저 ID를 추출
     * */
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }
}

