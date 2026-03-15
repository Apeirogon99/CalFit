package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.payment.toss.BillingKeyResponse;
import com.coffiness.calfit.domain.payment.toss.PaymentResponse;
import com.coffiness.calfit.domain.payment.toss.TossPaymentClient;
import com.coffiness.calfit.domain.payment.toss.TossPaymentProperties;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class TossPaymentClientImpl implements TossPaymentClient {

  private final RestTemplate restTemplate;
  private final TossPaymentProperties properties;
  private final String authorizationHeader;

  public TossPaymentClientImpl(TossPaymentProperties properties) {
    this.restTemplate = new RestTemplate();
    this.properties = properties;
    String credentials = properties.secretKey() + ":";
    this.authorizationHeader =
        "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public BillingKeyResponse issueBillingKey(String customerKey, String authKey) {
    String url = properties.apiBaseUrl() + "/v1/billing/authorizations/issue";

    Map<String, String> body = Map.of("customerKey", customerKey, "authKey", authKey);

    HttpHeaders headers = createHeaders();
    HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
      Map responseBody = response.getBody();
      log.info("Toss billing key response: {}", responseBody);
      if (responseBody == null) {
        throw new CoreException(ErrorType.DEFAULT_ERROR);
      }

      Map card = (Map) responseBody.get("card");
      log.info("Toss card info: {}", card);
      return new BillingKeyResponse(
          (String) responseBody.get("billingKey"),
          (String) responseBody.get("customerKey"),
          card != null ? (String) card.get("issuerCode") : null,
          card != null ? (String) card.get("number") : null,
          card != null ? (String) card.get("cardType") : null,
          card != null ? (String) card.get("ownerType") : null);
    } catch (RestClientException e) {
      log.error("Toss billing key issuance failed: {}", e.getMessage(), e);
      throw new CoreException(ErrorType.DEFAULT_ERROR);
    }
  }

  @Override
  public PaymentResponse chargeBillingKey(
      String billingKey, String customerKey, String orderId, Long amount, String orderName) {
    String url = properties.apiBaseUrl() + "/v1/billing/" + billingKey;

    Map<String, Object> body =
        Map.of(
            "customerKey", customerKey,
            "orderId", orderId,
            "amount", amount,
            "orderName", orderName);

    HttpHeaders headers = createHeaders();
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
      Map responseBody = response.getBody();
      if (responseBody == null) {
        throw new CoreException(ErrorType.DEFAULT_ERROR);
      }

      Map failure = (Map) responseBody.get("failure");
      return new PaymentResponse(
          (String) responseBody.get("paymentKey"),
          (String) responseBody.get("orderId"),
          (String) responseBody.get("status"),
          ((Number) responseBody.get("totalAmount")).longValue(),
          failure != null ? (String) failure.get("message") : null);
    } catch (RestClientException e) {
      log.error("Toss billing charge failed: {}", e.getMessage());
      return new PaymentResponse(null, orderId, "FAILED", amount, e.getMessage());
    }
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", authorizationHeader);
    return headers;
  }
}
