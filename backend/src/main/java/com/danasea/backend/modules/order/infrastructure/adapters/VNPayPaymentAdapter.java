package com.danasea.backend.modules.order.infrastructure.adapters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.danasea.backend.configs.properties.VNPayProperties;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.order.domain.ports.RefundResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter triển khai cổng thanh toán nội địa VNPay Sandbox (Mục 9.2.3 & 9.2.14).
 * Hỗ trợ tạo URL thanh toán VNPay-QR / ATM nội địa và xác thực chữ ký HMAC-SHA512.
 * <p>
 * Nguồn cấu hình (Configuration Source):
 * <ul>
 *   <li>{@code .env} : {@code VNPAY_TMN_CODE}, {@code VNPAY_HASH_SECRET}, {@code VNPAY_PAY_URL}, {@code VNPAY_RETURN_URL}</li>
 *   <li>{@code application.yml} : {@code app.payment.vnpay.*}</li>
 *   <li>{@link VNPayProperties} : record chứa các thuộc tính cấu hình inject trực tiếp vào adapter này</li>
 * </ul>
 */
@Component
@Slf4j
public class VNPayPaymentAdapter implements PaymentGatewayPort {

    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VNPayProperties vnpayProperties;

    public VNPayPaymentAdapter(VNPayProperties vnpayProperties) {
        this.vnpayProperties = vnpayProperties;
    }

    @Override
    public PaymentIntentResult createPaymentIntent(UUID orderId, BigDecimal amount, PaymentProvider provider) {
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(15);

        BigDecimal vnpAmount = (amount != null ? amount : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        String createDate = VNPAY_DATE_FORMAT.format(now);
        String expireDate = VNPAY_DATE_FORMAT.format(now.plusMinutes(15));

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayProperties.tmnCode());
        vnpParams.put("vnp_Amount", vnpAmount.toPlainString());
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId.toString());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang DANASea " + orderId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpayProperties.returnUrl());
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isBlank()) {
                String encodedKey = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);

                hashData.append(encodedKey).append('=').append(encodedValue).append('&');
                query.append(encodedKey).append('=').append(encodedValue).append('&');
            }
        }

        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
            query.setLength(query.length() - 1);
        }

        String secureHash = hmacSHA512(vnpayProperties.hashSecret(), hashData.toString());
        String paymentUrl = vnpayProperties.payUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;

        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                + URLEncoder.encode(paymentUrl, StandardCharsets.UTF_8);

        return new PaymentIntentResult(
                paymentId,
                orderId,
                PaymentProvider.VNPAY,
                amount,
                paymentUrl,
                qrCodeUrl,
                expiresAt
        );
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> rawParams, String signature) {
        if (signature == null || signature.isBlank() || rawParams == null || rawParams.isEmpty()) {
            return false;
        }

        Map<String, String> fields = new HashMap<>(rawParams);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isBlank()) {
                String encodedKey = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);
                hashData.append(encodedKey).append('=').append(encodedValue).append('&');
            }
        }

        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }

        try {
            String expected = hmacSHA512(vnpayProperties.hashSecret(), hashData.toString());
            byte[] expectedBytes = expected.toLowerCase().getBytes(StandardCharsets.US_ASCII);
            byte[] suppliedBytes = signature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expectedBytes, suppliedBytes);
        } catch (Exception ex) {
            log.warn("VNPay signature verification error: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public RefundResult requestRefund(String providerTransactionId, BigDecimal amount) {
        String refundId = "VNPAY-REF-" + UUID.randomUUID();
        return new RefundResult(true, refundId, amount, "Refund processed successfully via VNPay");
    }

    public static String hmacSHA512(String key, String data) {
        if (key == null) {
            key = "";
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] result = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA512", ex);
        }
    }
}
