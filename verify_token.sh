#!/bin/bash

# JWT Token Verification Script
# This script generates a test token and verifies its structure

echo "========================================="
echo "JWT Token Verification Report"
echo "========================================="
echo ""

# Generate a token using a simple Java invocation
cd /home/joao/workspace/k12/back

# Create a temporary Java file to generate and decode a token
cat > /tmp/TokenVerifier.java << 'EOF'
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.*;
import com.k12.user.infrastructure.security.TokenService;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import org.json.JSONObject;

public class TokenVerifier {
    public static void main(String[] args) {
        // Create test user
        User user = new User(
            new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
            new EmailAddress("admin@k12.com"),
            new PasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
            Set.of(UserRole.SUPER_ADMIN),
            UserStatus.ACTIVE,
            new UserName("System Administrator")
        );

        // Generate token
        String token = TokenService.generateToken(user, "default-tenant");

        System.out.println("TOKEN_GENERATED: " + token);
        System.out.println("");

        // Decode JWT parts
        String[] parts = token.split("\\.");
        if (parts.length >= 2) {
            // Decode header
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("JWT_HEADER: " + header);

            // Decode payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("JWT_PAYLOAD: " + payload);

            // Parse and extract claims
            JSONObject json = new JSONObject(payload);
            System.out.println("");
            System.out.println("CLAIM_VERIFICATION:");
            System.out.println("  sub (user ID): " + json.optString("sub", "MISSING"));
            System.out.println("  email: " + json.optString("email", "MISSING"));
            System.out.println("  roles: " + json.optString("roles", "MISSING"));
            System.out.println("  tenantId: " + json.optString("tenantId", "MISSING"));
            System.out.println("  iss (issuer): " + json.optString("iss", "MISSING"));
            System.out.println("  iat (issued at): " + json.optLong("iat", -1));
            System.out.println("  exp (expires at): " + json.optLong("exp", -1));

            // Calculate hours until expiry
            long currentTime = System.currentTimeMillis() / 1000;
            long expiryTime = json.optLong("exp", -1);
            long hoursUntilExpiry = (expiryTime - currentTime) / 3600;
            System.out.println("  Hours until expiry: " + hoursUntilExpiry);
        }
    }
}
EOF

# Compile and run
echo "Generating and decoding token..."
echo ""

javac -cp "build/classes/java/main:$(find ~/.gradle/caches -name 'smallrye-jwt-*.jar' | head -1):$(find ~/.gradle/caches -name 'jakarta.json-api-*.jar' | head -1)" /tmp/TokenVerifier.java 2>&1 | head -20

if [ $? -eq 0 ]; then
    java -cp "/tmp:build/classes/java/main:$(find ~/.gradle/caches -name 'smallrye-jwt-*.jar' | head -1):$(find ~/.gradle/caches -name 'jakarta.json-api-*.jar' | head -1):$(find ~/.gradle/caches -name 'jsonp-impl-*.jar' | head -1)" TokenVerifier
else
    echo "Compilation failed. Using alternative method..."
    echo ""
    echo "Please run the TokenServiceTest which already verifies token structure:"
    echo "./gradlew test --tests TokenServiceTest"
fi

echo ""
echo "========================================="
echo "Verification Complete"
echo "========================================="
