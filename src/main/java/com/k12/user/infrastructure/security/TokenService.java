package com.k12.user.infrastructure.security;

import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class TokenService {
    private static final String ISSUER = "k12-api";
    private static final long TOKEN_VALIDITY_HOURS = 24;

    public static String generateToken(User user, String tenantId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(TOKEN_VALIDITY_HOURS * 3600);
        Set<String> roles = user.userRole().stream().map(UserRole::name).collect(Collectors.toSet());

        return Jwt.claims()
                .subject(user.userId().value().toString())
                .claim("email", user.emailAddress().value())
                .claim("groups", roles) // TEMPORARY: Test with "groups" claim
                .claim("roles", roles) // Keep roles for our SecurityContext
                .claim("tenantId", tenantId)
                .issuedAt(now)
                .expiresAt(exp)
                .issuer(ISSUER)
                .jws()
                .keyId("1")
                .sign(getPrivateKey());
    }

    public static String extractClaim(String token, String claimName) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String search = "\"" + claimName + "\":\"";
            int start = payload.indexOf(search);
            if (start == -1) return null;
            start += search.length();
            int end = payload.indexOf("\"", start);
            if (end == -1) return null;
            return payload.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private static PrivateKey getPrivateKey() {
        try {
            String keyPath = System.getenv().getOrDefault("JWT_KEY_PATH", "/app/keys/private-key.pem");
            String keyContent = new String(Files.readAllBytes(Paths.get(keyPath)), StandardCharsets.UTF_8);
            keyContent = keyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key", e);
        }
    }
}
