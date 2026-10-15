package com.reglog.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.reglog.config.DatabaseConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class JwtUtil {
    private static final Gson gson = new Gson();

    public static class JwtClaims {
        public Long userId;
        public String email;
        public String mobile;
        public String jti; // token identifier for session management
        public long exp;
    }

    public static String generateToken(Long userId, String email, String mobile, String jti) {
        long now = System.currentTimeMillis();
        long exp = now + DatabaseConfig.getJwtExpirationMs();

        JsonObject header = new JsonObject();
        header.addProperty("alg", "HS256");
        header.addProperty("typ", "JWT");

        JsonObject payload = new JsonObject();
        payload.addProperty("sub", String.valueOf(userId));
        payload.addProperty("email", email);
        payload.addProperty("mobile", mobile);
        payload.addProperty("jti", jti);
        payload.addProperty("iat", now / 1000);
        payload.addProperty("exp", exp / 1000);

        String encodedHeader = base64UrlEncode(header.toString());
        String encodedPayload = base64UrlEncode(payload.toString());

        String dataToSign = encodedHeader + "." + encodedPayload;
        String signature = hmacSha256(dataToSign, DatabaseConfig.getJwtSecret());

        return dataToSign + "." + signature;
    }

    public static JwtClaims validateAndParseToken(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String dataToSign = parts[0] + "." + parts[1];
        String expectedSignature = hmacSha256(dataToSign, DatabaseConfig.getJwtSecret());

        if (!parts[2].equals(expectedSignature)) {
            return null; // invalid signature
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject obj = gson.fromJson(payloadJson, JsonObject.class);

            long expSeconds = obj.get("exp").getAsLong();
            if (System.currentTimeMillis() / 1000 > expSeconds) {
                return null; // token expired
            }

            JwtClaims claims = new JwtClaims();
            claims.userId = Long.parseLong(obj.get("sub").getAsString());
            claims.email = obj.get("email").getAsString();
            claims.mobile = obj.get("mobile").getAsString();
            claims.jti = obj.get("jti").getAsString();
            claims.exp = expSeconds * 1000;
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    private static String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error computing HMAC-SHA256", e);
        }
    }
}
