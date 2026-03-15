package com.coffiness.calfit.support.security.jwt;

/** Security Context에 저장되는 인증된 사용자 정보 */
public record SecurityUser(Long userId, String email, String role) {}
