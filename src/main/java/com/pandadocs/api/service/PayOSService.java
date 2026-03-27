package com.pandadocs.api.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pandadocs.api.config.PayOSConfig;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

@Service
@Slf4j
public class PayOSService {

    @Autowired
    private PayOSConfig payOSConfig;

    private PayOS payOS;

    @PostConstruct
    public void init() {
        payOS = new PayOS(payOSConfig.getClientId(), payOSConfig.getApiKey(), payOSConfig.getChecksumKey());
    }

    public CreatePaymentLinkResponse createPaymentLink(Long orderCode, Long amount, String description,
                                                       List<PaymentLinkItem> items, String cancelUrl, String returnUrl) throws Exception {
        log.info("Creating PayOS payment link for order: {}", orderCode);

        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .items(items)
                .cancelUrl(cancelUrl)
                .returnUrl(returnUrl)
                .build();

        return payOS.paymentRequests().create(request);
    }

    public boolean verifyWebhookSignature(String data, String signature) {
        if (!StringUtils.hasText(data) || !StringUtils.hasText(signature) || !StringUtils.hasText(payOSConfig.getChecksumKey())) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(payOSConfig.getChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expectedSignature = HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
            String providedSignature = signature.trim().toLowerCase(Locale.ROOT);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (GeneralSecurityException e) {
            log.error("Unable to verify PayOS webhook signature", e);
            return false;
        }
    }

    public PaymentLink getPaymentInfo(Long orderCode) throws Exception {
        return payOS.paymentRequests().get(orderCode);
    }
}
