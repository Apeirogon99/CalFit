package com.coffiness.calfit.domain.notification;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/*
 * 워크스페이스 대시보드의 익명 채팅 메시지를 메모리 기반으로 관리
 * */
@Service
public class DashboardChatService {

  private static final int MAX_RECENT_MESSAGES = 50;
  private static final List<String> ALIAS_COLORS =
      List.of("민트", "코랄", "네이비", "골드", "라임", "스카이", "루비", "모카", "라벤더", "오션");

  private final AtomicLong sequence = new AtomicLong(1L);
  private final Map<String, Deque<DashboardChatMessage>> messagesByTenant =
      new ConcurrentHashMap<>();

  // 워크스페이스의 최근 채팅 메시지를 반환
  public List<DashboardChatMessage> getRecentMessages(String tenantId) {
    Deque<DashboardChatMessage> messages = messagesByTenant.get(tenantId);
    if (messages == null) {
      return List.of();
    }

    synchronized (messages) {
      return new ArrayList<>(messages);
    }
  }

  // 새 채팅 메시지를 저장하고 최근 메시지 버퍼를 유지
  public DashboardChatMessage createMessage(String tenantId, Long userId, String content) {
    Deque<DashboardChatMessage> messages =
        messagesByTenant.computeIfAbsent(tenantId, ignored -> new ArrayDeque<>());

    DashboardChatMessage message =
        new DashboardChatMessage(
            sequence.getAndIncrement(),
            userId,
            createAlias(userId),
            normalizeContent(content),
            LocalDateTime.now());

    synchronized (messages) {
      messages.addLast(message);
      while (messages.size() > MAX_RECENT_MESSAGES) {
        messages.removeFirst();
      }
    }

    return message;
  }

  // 로그인 사용자를 기준으로 익명 별칭을 고정 생성
  private String createAlias(Long userId) {
    long safeUserId = userId == null ? 0L : Math.abs(userId);
    String color = ALIAS_COLORS.get((int) (safeUserId % ALIAS_COLORS.size()));
    String code = Long.toString((safeUserId * 37L) + 11L, 36).toUpperCase();
    String suffix = code.length() <= 2 ? code : code.substring(code.length() - 2);
    return "익명 " + color + "-" + suffix;
  }

  // 저장 전에 메시지 내용을 정리
  private String normalizeContent(String content) {
    return String.valueOf(content).replace("\r\n", "\n").replace('\r', '\n').trim();
  }

  /* 대시보드 익명 채팅의 내부 메시지 모델 */
  public record DashboardChatMessage(
      Long id, Long userId, String alias, String content, LocalDateTime createdAt) {}
}
