package com.coffiness.calfit.domain.payment.toss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.payments")
public record TossPaymentProperties(String clientKey, String secretKey, String apiBaseUrl) {}
