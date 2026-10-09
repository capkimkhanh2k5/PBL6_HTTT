package com.danasea.backend.modules.order.infrastructure.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.danasea.backend.configs.properties.VNPayProperties;
import com.danasea.backend.modules.order.domain.exceptions.PaymentGatewayException;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.GatewayRefundRequest;
import com.danasea.backend.modules.order.domain.ports.GatewayRefundStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

class VNPayPaymentAdapterTest {
    private static final String API = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private static final String SECRET = "test-only-vnpay-signing-secret-32-chars";
    private final ObjectMapper mapper = new ObjectMapper();
    private VNPayPaymentAdapter adapter;
    private MockRestServiceServer server;
    private GatewayRefundRequest request;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new VNPayPaymentAdapter(new VNPayProperties("TESTCODE", SECRET,
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", null, API), builder, mapper);
        request = new GatewayRefundRequest(PaymentProvider.VNPAY, "987654321", "original-payment-uuid", "20261005123045",
                new BigDecimal("200000"), "VND", false, "67162a2f-4d13-4e70-ae56-d8dba6d41f4d");
    }

    @Test
    void createIntentPersistsTheOriginalReferenceDateAndGatewayMoney() {
        UUID paymentId = UUID.randomUUID();
        var result = adapter.createPaymentIntent(paymentId, UUID.randomUUID(), new BigDecimal("500000"), PaymentProvider.VNPAY);
        assertThat(result.providerOrderId()).isEqualTo(paymentId.toString());
        assertThat(result.providerTransactionDate()).matches("[0-9]{14}");
        assertThat(result.providerAmount()).isEqualByComparingTo("500000");
        assertThat(result.providerCurrency()).isEqualTo("VND");
        assertThat(result.paymentUrl()).contains("vnp_TxnRef=" + paymentId, "vnp_CreateDate=" + result.providerTransactionDate(), "vnp_Amount=50000000", "vnp_SecureHash=");
    }

    @Test
    void validSignedIpnIsAcceptedAndTamperedDataOrMerchantIsRejected() throws Exception {
        Map<String, String> params = new HashMap<>(Map.of("vnp_Amount", "50000000", "vnp_TmnCode", "TESTCODE"));
        String signature = hmac("vnp_Amount=50000000&vnp_TmnCode=TESTCODE");
        assertThat(adapter.verifyWebhookSignature(params, signature)).isTrue();
        params.put("vnp_Amount", "1");
        assertThat(adapter.verifyWebhookSignature(params, signature)).isFalse();
        params.put("vnp_TmnCode", "OTHER");
        assertThat(adapter.verifyWebhookSignature(params, hmac("vnp_Amount=1&vnp_TmnCode=OTHER"))).isFalse();
    }

    @Test
    void partialRefundUsesOriginalTxnRefDateTypeAndStableFinancialRequestId() throws Exception {
        server.expect(requestTo(API)).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"vnp_TxnRef\":\"original-payment-uuid\",\"vnp_TransactionNo\":\"987654321\","
                        + "\"vnp_TransactionDate\":\"20261005123045\",\"vnp_TransactionType\":\"03\",\"vnp_Amount\":\"20000000\","
                        + "\"vnp_RequestId\":\"67162a2f4d134e70ae56d8dba6d41f4d\"}"))
                .andRespond(withSuccess(response("refund", "03", "00", "00", "REFUND999"), MediaType.APPLICATION_JSON));
        var result = adapter.requestRefund(request);
        assertThat(result.success()).isTrue();
        assertThat(result.providerRefundId()).isEqualTo("REFUND999");
        assertThat(result.amount()).isEqualByComparingTo("200000");
        server.verify();
    }

    @Test
    void originalPaymentSuccessDoesNotProveRefundSuccess() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "01", "00", "00", "987654321"), MediaType.APPLICATION_JSON));
        var result = adapter.queryRefund(request, null);
        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo(GatewayRefundStatus.UNKNOWN);
        server.verify();
    }

    @Test
    void queryPartialRefundRequiresTheExactKnownRefundId() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "03", "00", "00", "ANOTHER-REFUND"), MediaType.APPLICATION_JSON));
        assertThat(adapter.queryRefund(request, "REFUND999").status()).isEqualTo(GatewayRefundStatus.UNKNOWN);
        server.verify();
    }

    @Test
    void queryPartialRefundWithoutIdCannotConfuseTwoEqualRefunds() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "03", "00", "00", "REFUND999"), MediaType.APPLICATION_JSON));
        assertThat(adapter.queryRefund(request, null).status()).isEqualTo(GatewayRefundStatus.UNKNOWN);
        server.verify();
    }

    @Test
    void signedMatchingRefundQueryCanCompleteTheOperation() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "03", "00", "00", "REFUND999"), MediaType.APPLICATION_JSON));
        assertThat(adapter.queryRefund(request, "REFUND999").status()).isEqualTo(GatewayRefundStatus.COMPLETED);
        server.verify();
    }

    @Test
    void refundStillProcessingDoesNotBecomeCompleted() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("refund", "03", "94", "05", "REFUND999"), MediaType.APPLICATION_JSON));
        var result = adapter.requestRefund(request);
        assertThat(result.status()).isEqualTo(GatewayRefundStatus.PENDING);
        assertThat(result.success()).isFalse();
        server.verify();
    }

    @Test
    void invalidOrMissingApiResponseChecksumIsRejected() {
        server.expect(requestTo(API)).andRespond(withSuccess("{\"vnp_ResponseCode\":\"00\",\"vnp_TransactionStatus\":\"00\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.requestRefund(request)).isInstanceOf(PaymentGatewayException.class);
        server.verify();
    }

    @Test
    void refundCannotInventAnOriginalTransactionDate() {
        var missingDate = new GatewayRefundRequest(PaymentProvider.VNPAY, "987654321", "original-payment-uuid", null,
                request.amount(), "VND", false, request.requestId());
        assertThatThrownBy(() -> adapter.requestRefund(missingDate)).isInstanceOf(PaymentGatewayException.class);
        server.verify();
    }

    private String response(String command, String type, String code, String status, String transactionId) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_ResponseId", "RESPONSE-ID-NOT-REFUND-ID"); body.put("vnp_Command", command);
        body.put("vnp_ResponseCode", code); body.put("vnp_Message", "Result"); body.put("vnp_TmnCode", "TESTCODE");
        body.put("vnp_TxnRef", request.orderId()); body.put("vnp_Amount", "20000000"); body.put("vnp_BankCode", "NCB");
        body.put("vnp_PayDate", "20261005123045"); body.put("vnp_TransactionNo", transactionId);
        body.put("vnp_TransactionType", type); body.put("vnp_TransactionStatus", status); body.put("vnp_OrderInfo", "Refund");
        if (command.equals("querydr")) { body.put("vnp_PromotionCode", ""); body.put("vnp_PromotionAmount", ""); }
        body.put("vnp_SecureHash", hmac(String.join("|", body.values())));
        return mapper.writeValueAsString(body);
    }

    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
    @Test
    void reviewSignedResponseForAnotherReferenceCannotCompletePayment() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "01", "00", "00", "987654321"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.queryPayment(PaymentProvider.VNPAY, "different-request-reference", "20261005123045"))
            .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    void reviewSignedRefundResponseCannotProvePaymentCompletion() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "03", "00", "00", "REFUND999"), MediaType.APPLICATION_JSON));
        assertThat(adapter.queryPayment(PaymentProvider.VNPAY, request.orderId(), request.transactionDate()).success()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"04", "07", "09"})
    void ambiguousOrRefundRelatedTransactionStatusesDoNotFailAPayment(String status) throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "01", "00", status, "987654321"), MediaType.APPLICATION_JSON));
        var result = adapter.queryPayment(PaymentProvider.VNPAY, request.orderId(), request.transactionDate());
        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo("UNKNOWN");
    }

    @Test
    void failedLookupCannotBeMistakenForFailedPayment() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "01", "94", "02", "987654321"), MediaType.APPLICATION_JSON));
        assertThat(adapter.queryPayment(PaymentProvider.VNPAY, request.orderId(), request.transactionDate()).status()).isEqualTo("UNKNOWN");
    }

    @Test
    void matchingPaymentQueryReturnsGatewayMoney() throws Exception {
        server.expect(requestTo(API)).andRespond(withSuccess(response("querydr", "01", "00", "00", "987654321"), MediaType.APPLICATION_JSON));
        var result = adapter.queryPayment(PaymentProvider.VNPAY, request.orderId(), request.transactionDate());
        assertThat(result.success()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("200000");
        assertThat(result.currency()).isEqualTo("VND");
        server.verify();
    }
}
