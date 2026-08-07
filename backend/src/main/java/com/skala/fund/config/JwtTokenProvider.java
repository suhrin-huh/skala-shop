package com.skala.fund.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
                            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String createAccessToken(Long customerId) {
        return createToken(customerId, accessTokenExpirationMs);
    }

    public String createRefreshToken(Long customerId) {
        return createToken(customerId, refreshTokenExpirationMs);
    }

    private String createToken(Long customerId, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(customerId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** 토큰이 유효하면 회원 ID, 아니면 null. 만료·위조는 정상적인 흐름이므로 예외로 올리지 않는다. */
    public Long parseCustomerId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 토큰: {}", e.getMessage());
            return null;
        }
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public LocalDateTime getRefreshTokenExpiresAt() {
        return LocalDateTime.ofInstant(
                new Date(System.currentTimeMillis() + refreshTokenExpirationMs).toInstant(),
                ZoneId.systemDefault());
    }
}
