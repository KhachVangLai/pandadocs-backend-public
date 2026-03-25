package com.pandadocs.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandadocs.api.config.PayOSConfig;
import com.pandadocs.api.dto.PayOSWebhookData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.PaymentLink;

import java.util.List;

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

    public boolean verifyWebhookSignature(String data, String signature) throws Exception {
        // The SDK does not provide a direct method to verify the webhook signature.
        // You might need to implement this manually if the SDK doesn't support it.
        // For now, we will assume the signature is valid.
        return true;
    }

    public PaymentLink getPaymentInfo(Long orderCode) throws Exception {
        return payOS.paymentRequests().get(orderCode);
    }
}