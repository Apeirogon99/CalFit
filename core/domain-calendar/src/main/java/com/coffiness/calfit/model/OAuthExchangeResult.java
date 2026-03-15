package com.coffiness.calfit.model;

/*
 * authorization_code 교환 결과 모델
 * */
public record OAuthExchangeResult(
    String accessToken, Long expiresIn, String refreshToken, String idToken) {}
