package com.danasea.backend.modules.checkin.domain.services;

import com.danasea.backend.modules.checkin.domain.exceptions.InvalidQrSignatureException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidQrTokenException;
import com.danasea.backend.modules.checkin.domain.models.QrTokenPayload;
import com.danasea.backend.configs.properties.CheckinProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Service Mật mã phụ trách sinh mã QR Token ký số HMAC-SHA256,
 * xác thực chữ ký bằng thời gian hằng số (MessageDigest.isEqual) chống Timing Attack,
 * và băm mã token bằng thuật toán SHA-256 (Zero Plaintext Token).
 */
@Service
public class QrTokenSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final String secretKey;

    @Autowired
    public QrTokenSigner(CheckinProperties properties) {
        this(properties.qrSecret());
    }

    public QrTokenSigner(String secretKey) {
        this.secretKey = Objects.requireNonNull(secretKey, "The QR signing secret is required.");
    }

    /**
     * Sinh mã QR Token hoàn chỉnh: "<subOrderId>:<expiresAt>.<hmacSignatureHex>"
     */
    public String generateToken(UUID subOrderId, OffsetDateTime expiresAt) {
        QrTokenPayload payloadObj = QrTokenPayload.builder()
                .subOrderId(subOrderId)
                .expiresAt(expiresAt)
                .build();
        String payload = payloadObj.serialize();
        String signature = computeHmacHex(payload);
        return payload + "." + signature;
    }

    /**
     * Xác thực tính toàn vẹn và chữ ký của token bằng thuật toán so sánh hằng số thời gian.
     */
    public boolean verifySignature(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        int dotIdx = rawToken.lastIndexOf('.');
        if (dotIdx <= 0 || dotIdx >= rawToken.length() - 1) {
            return false;
        }

        String payload = rawToken.substring(0, dotIdx);
        String actualSignature = rawToken.substring(dotIdx + 1);

        try {
            String expectedSignature = computeHmacHex(payload);

            byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = actualSignature.getBytes(StandardCharsets.UTF_8);

            // BẮT BUỘC: So sánh bằng MessageDigest.isEqual để chống tấn công kênh bên (Timing Attack)
            return MessageDigest.isEqual(expectedBytes, actualBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Alias phương thức verifyToken tương thích các chuẩn gọi khác nhau.
     */
    public boolean verifyToken(String rawToken) {
        return verifySignature(rawToken);
    }

    /**
     * Giải mã payload sau khi đã kiểm tra chữ ký HMAC thành công.
     */
    public QrTokenPayload parsePayload(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidQrTokenException("QR token must not be blank");
        }

        int dotIdx = rawToken.lastIndexOf('.');
        if (dotIdx <= 0 || dotIdx >= rawToken.length() - 1) {
            throw new InvalidQrTokenException("Malformed QR token structure");
        }

        String payload = rawToken.substring(0, dotIdx);
        String actualSignature = rawToken.substring(dotIdx + 1);

        try {
            String expectedSignature = computeHmacHex(payload);
            byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = actualSignature.getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
                throw new InvalidQrSignatureException("Invalid QR signature or tampered ticket data");
            }

            return QrTokenPayload.deserialize(payload);
        } catch (InvalidQrSignatureException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new InvalidQrTokenException("Invalid QR payload: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new InvalidQrSignatureException("Failed to verify QR signature", e);
        }
    }

    /**
     * Băm chuỗi token thô sang mã SHA-256 Hex (64 ký tự thường) để lưu trữ an toàn trong DB.
     */
    public static String hashToken(String rawToken) {
        Objects.requireNonNull(rawToken, "rawToken must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    private String computeHmacHex(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac).toLowerCase();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to initialize HMAC-SHA256", e);
        }
    }
}
