package io.github.seoleeder.owls_pick.global.security.jwt;

import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.global.security.config.properties.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 요청 헤더에서 토큰 추출 (예외 상황 처리 포함)
        String token = resolveToken(request);

        try {
            // 토큰이 존재할 때만 검증 로직 수행
            if (token != null) {
                // 토큰 유효성 검증
                tokenProvider.validateToken(token);

                // 유효하면 인증 객체 생성 및 Context 저장
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e){
            log.info("만료된 JWT 토큰입니다.");
            // Request Scope에 예외 원인 저장
            request.setAttribute("exception", ErrorCode.EXPIRED_TOKEN);
        } catch (MalformedJwtException | SignatureException | UnsupportedJwtException | IllegalArgumentException e){
            log.warn("유효하지 않은 JWT 토큰입니다.");
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN); // 40102 (로그인 대상)
        } catch (Exception e) {
            log.error("JWT 검증 중 알 수 없는 에러", e);
            request.setAttribute("exception", ErrorCode.UNAUTHORIZED);
        }

        // 다음 필터로 진행
        // 예외가 발생했더라도, SecurityContext가 비어있는 상태로 다음 체인으로 진행
        // (이후 FilterSecurityInterceptor가 인증 없음을 감지하고 EntryPoint 호출)
        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 토큰 정보 꺼내오기
     * - "Bearer " 접두사 체크
     * - null, 공백 체크 등 방어 로직 추가
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.header());

        // Authorization 헤더가 존재하는지 검증
        if (!StringUtils.hasText(bearerToken)) {
            log.debug("Authorization 헤더가 없는 요청입니다. URI: {}", request.getRequestURI());
            return null;
        }

        // 토큰이 "Bearer "로 시작하는지 검증
        if (!bearerToken.startsWith("Bearer ")) {
            log.warn("지원하지 않는 인증 방식이거나 잘못된 헤더 형식입니다. Header: {}", bearerToken);
            return null;
        }

        // "Bearer " 뒤에 토큰 값이 존재하는지 검증
        if (bearerToken.length() == 7) {
            log.warn("Bearer 헤더에 토큰 값이 비어있습니다.");
            return null;
        }

        // "Bearer " 이후의 문자열(토큰)만 반환
        return bearerToken.substring(7);
    }
}
