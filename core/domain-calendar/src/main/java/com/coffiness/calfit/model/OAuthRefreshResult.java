package com.coffiness.calfit.model;

/*
 * refresh_token 교환 결과 모델
 * */
public record OAuthRefreshResult(String accessToken, Long expiresIn, String refreshToken) {}
