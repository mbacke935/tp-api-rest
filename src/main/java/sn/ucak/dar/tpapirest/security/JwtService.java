package sn.ucak.dar.tpapirest.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.ucak.dar.tpapirest.entities.User;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final String secretBase64;
    private final long ttlSeconds;
    private SecretKey signingKey;

    public JwtService(
        @Value("${app.jwt.secret-base64}") String secretBase64,
        @Value("${app.jwt.ttl-seconds:900}") long ttlSeconds
    ) {
        this.secretBase64 = secretBase64;
        this.ttlSeconds = ttlSeconds;
    }

    @PostConstruct
    public void init() {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("La variable JWT_SECRET_BASE64 est obligatoire et ne peut pas être vide.");
        }
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(secretBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET_BASE64 n'est pas une chaîne Base64 valide.", e);
        }
        if (decodedKey.length < 32) {
            throw new IllegalStateException("La clé JWT décodée doit contenir au moins 32 octets (256 bits).");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalStateException("JWT_TTL_SECONDS doit être strictement positif.");
        }
        this.signingKey = Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
            .subject(user.getId().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    public UUID extractUserIdIfValid(String token) {
        try {
            var jws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);

            String alg = jws.getHeader().getAlgorithm();
            if (!"HS256".equals(alg)) {
                return null;
            }
            Claims claims = jws.getPayload();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return null;
            }
            return UUID.fromString(subject);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
