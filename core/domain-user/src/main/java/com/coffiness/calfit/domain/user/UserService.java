package com.coffiness.calfit.domain.user;

import com.coffiness.calfit.domain.user.event.UserDeletedEvent;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import com.coffiness.calfit.support.security.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;

  private final MemberRepository memberRepository;

  private final PasswordEncoder passwordEncoder;

  private final JwtTokenProvider jwtTokenProvider;

  private final DomainEventPublisher eventPublisher;

  public UserService(
      UserRepository userRepository,
      MemberRepository memberRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      DomainEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.memberRepository = memberRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public User signUp(String email, String password, String name) {
    if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + email);
    }

    String encodedPassword = passwordEncoder.encode(password);
    UserEntity userEntity = UserEntity.createMember(email, encodedPassword, name);
    UserEntity savedEntity = userRepository.save(userEntity);

    return toUser(savedEntity);
  }

  @Transactional(readOnly = true)
  public User getUserByEmail(String email) {
    UserEntity userEntity =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

    return toUser(userEntity);
  }

  @Transactional(readOnly = true)
  public LoginResult login(String email, String password) {
    UserEntity userEntity =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

    if (!passwordEncoder.matches(password, userEntity.getPassword())) {
      throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    String accessToken =
        jwtTokenProvider.createAccessToken(
            userEntity.getId(), userEntity.getEmail(), userEntity.getRole().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(userEntity.getId());

    String workspaceId = memberRepository.findTenantIdByUserId(userEntity.getId()).orElse(null);

    return new LoginResult(accessToken, refreshToken, toUser(userEntity), workspaceId);
  }

  @Transactional(readOnly = true)
  public String getWorkspaceId(Long userId) {
    return memberRepository.findTenantIdByUserId(userId).orElse(null);
  }

  @Transactional(readOnly = true)
  public User getUser(Long userId) {
    UserEntity userEntity =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

    return toUser(userEntity);
  }

  @Transactional
  public void deleteUser(Long userId) {
    UserEntity userEntity =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

    String email = userEntity.getEmail();
    userRepository.delete(userEntity);

    // 회원 탈퇴 이벤트 발행
    eventPublisher.publish(UserDeletedEvent.of(userId, email));
  }

  private User toUser(UserEntity entity) {
    return new User(
        entity.getId(),
        entity.getEmail(),
        entity.getName(),
        entity.getRole().name(),
        entity.getCreatedAt());
  }

  public record LoginResult(
      String accessToken, String refreshToken, User user, String workspaceId) {}
}
