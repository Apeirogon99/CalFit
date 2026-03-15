package com.coffiness.calfit.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * 구글 watch 채널 토큰을 생성하고 검증하는 서비스
 * */
@Service
public class GoogleChannelTokenService {

  private static final String VERSION_PREFIX = "v1";
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final String watchTokenSecret;

  // 채널 토큰 서명에 사용할 비밀키를 주입받아 초기화
  public GoogleChannelTokenService(
      @Value("${calendar.google-sync.watch-token-secret:calfit-google-watch-secret}")
          String watchTokenSecret) {
    this.watchTokenSecret = watchTokenSecret;
  }

  // tenantId와 externalCalendarId를 서명된 채널 토큰 문자열로 생성
  public String createToken(String tenantId, Long externalCalendarId) {
    if (tenantId == null || tenantId.isBlank() || externalCalendarId == null) {
      throw new IllegalArgumentException("GOOGLE_CHANNEL_TOKEN_INVALID_PAYLOAD");
    }

    String payload = tenantId + ":" + externalCalendarId;
    String payloadEncoded = encodeBase64Url(payload.getBytes(StandardCharsets.UTF_8));
    String signature = sign(payloadEncoded);

    return VERSION_PREFIX + "." + payloadEncoded + "." + signature;
  }

  // 채널 토큰의 형식과 서명을 검증한 뒤 payload를 파싱해 반환
  public ChannelTokenPayload parseToken(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("GOOGLE_CHANNEL_TOKEN_MISSING");
    }

    String[] split = token.split("\\.");
    if (split.length != 3 || !VERSION_PREFIX.equals(split[0])) {
      throw new IllegalArgumentException("GOOGLE_CHANNEL_TOKEN_INVALID_FORMAT");
    }

    String payloadEncoded = split[1];
    String receivedSignature = split[2];
    String expectedSignature = sign(payloadEncoded);

    if (!secureEquals(expectedSignature, receivedSignature)) {
      throw new IllegalArgumentException("GOOGLE_CHANNEL_TOKEN_INVALID_SIGNATURE");
    }

    String payload;
    try {
      payload = new String(decodeBase64Url(payloadEncoded), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("GOOGLE_CHANNEL_TOKEN_INVALID_PAYLOAD", e);
    }

    int separatorIndex = payload.lastIndexOf(':');
    if (separatorIndex <= 0 || separatorIndex == payload.length() - 1) {
      throw new IllegalArgumentException("GOOGLE_CHANNEL_TOKEN_INVALID_PAYLOAD");
    }

    String tenantId = payload.substring(0, separatorIndex);
    Long externalCalendarId;
    try {
      externalCalendarId = Long.parseLong(payload.substring(separatorIndex + 1));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("GOOGLE_CHANNEL_TOKEN_INVALID_PAYLOAD", e);
    }

    return new ChannelTokenPayload(tenantId, externalCalendarId);
  }

  // payload를 HMAC-SHA256으로 서명해 URL-safe Base64 문자열로 변환
  private String sign(String payloadEncoded) {
    if (watchTokenSecret == null || watchTokenSecret.isBlank()) {
      throw new IllegalStateException("GOOGLE_CHANNEL_TOKEN_SECRET_MISSING");
    }

    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(
          new SecretKeySpec(watchTokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      byte[] signature = mac.doFinal(payloadEncoded.getBytes(StandardCharsets.UTF_8));

      return encodeBase64Url(signature);
    } catch (Exception e) {
      throw new IllegalStateException("GOOGLE_CHANNEL_TOKEN_SIGNING_FAILED", e);
    }
  }

  // 바이트 배열을 패딩 없는 URL-safe Base64 문자열로 인코딩
  private String encodeBase64Url(byte[] input) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
  }

  // URL-safe Base64 문자열을 바이트 배열로 디코딩
  private byte[] decodeBase64Url(String input) {
    return Base64.getUrlDecoder().decode(input);
  }

  // 타이밍 공격 완화를 위해 상수 시간 비교 방식으로 문자열을 비교
  private boolean secureEquals(String expected, String actual) {
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  // 검증된 채널 토큰 payload(tenantId, externalCalendarId)를 담는 모델
  public record ChannelTokenPayload(String tenantId, Long externalCalendarId) {}
}
