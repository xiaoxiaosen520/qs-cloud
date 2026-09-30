package com.qs.takeout.common.jwt;

import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String createToken(AuthUser user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole())
                .claim("phone", user.getPhone() == null ? "" : user.getPhone())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getExpireHours(), ChronoUnit.HOURS)));
        if (user.getShopId() != null) {
            builder.claim("shopId", user.getShopId());
        }
        return builder.signWith(key).compact();
    }

    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long shopId = claims.get("shopId", Long.class);
            return AuthUser.builder()
                    .id(Long.valueOf(claims.getSubject()))
                    .role(claims.get("role", String.class))
                    .phone(claims.get("phone", String.class))
                    .shopId(shopId)
                    .build();
        } catch (Exception e) {
            throw new BizException(401, "登录已失效");
        }
    }
}
