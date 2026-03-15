package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.payment.billingkey.BillingKeyInfo;

public record CardInfoResponse(
    String cardCompany, String cardNumber, String cardType, String ownerType) {

  public static CardInfoResponse from(BillingKeyInfo info) {
    return new CardInfoResponse(
        info.cardCompany(), info.cardNumber(), info.cardType(), info.ownerType());
  }
}
