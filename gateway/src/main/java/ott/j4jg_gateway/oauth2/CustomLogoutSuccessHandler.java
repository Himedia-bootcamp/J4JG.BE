package ott.j4jg_gateway.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import ott.j4jg_gateway.jwt.JWTUtil;
import ott.j4jg_gateway.repository.RedisRefreshTokenRepository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component
public class CustomLogoutSuccessHandler implements ServerLogoutSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String LOGOUT_SUCCESS_MESSAGE = "로그아웃 성공";

    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final JWTUtil jwtUtil;

    @Autowired
    public CustomLogoutSuccessHandler(RedisRefreshTokenRepository redisRefreshTokenRepository, JWTUtil jwtUtil) {
        this.redisRefreshTokenRepository = redisRefreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        String accessTokenValue = extractTokenFromCookie(exchange, ACCESS_TOKEN_COOKIE_NAME);

        Mono<Void> deleteTokensMono = Mono.empty();

        if (accessTokenValue != null) {
            String providerId = jwtUtil.getProviderId(accessTokenValue);
            log.debug("Extracted providerId from access token: {}", providerId);

            if (providerId != null) {
                deleteTokensMono = redisRefreshTokenRepository.findById(providerId)
                        .flatMap(refreshToken -> {
                            log.info("Redis에서 리프레시 토큰을 찾았습니다: {}", refreshToken);
                            return redisRefreshTokenRepository.deleteById(providerId);
                        })
                        .doOnSuccess(deleted -> log.info(deleted
                                ? "리프레시 토큰이 Redis에서 삭제되었습니다: {}"
                                : "리프레시 토큰이 Redis에서 삭제되지 않았습니다: {}", providerId))
                        .doOnError(e -> log.error("리프레시 토큰 삭제 중 오류 발생", e)).then();
            } else {
                log.warn("providerId가 null입니다. 리프레시 토큰을 삭제할 수 없습니다.");
            }
        } else {
            log.warn("액세스 토큰이 null입니다. 쿠키에서 액세스 토큰을 찾을 수 없습니다.");
        }

        deleteTokensMono = deleteTokensMono.then(Mono.fromRunnable(() -> {
            ResponseCookie accessCookie = deleteCookie(ACCESS_TOKEN_COOKIE_NAME);
            exchange.getResponse().addCookie(accessCookie);
        }));

        return deleteTokensMono.then(Mono.defer(() -> writeResponse(exchange, LOGOUT_SUCCESS_MESSAGE)));
    }

    private String extractTokenFromCookie(ServerWebExchange exchange, String cookieName) {
        return Optional.ofNullable(exchange.getRequest().getCookies().getFirst(cookieName))
                .map(cookie -> cookie.getValue())
                .orElse(null);
    }

    private ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(message.getBytes())));
    }
}
