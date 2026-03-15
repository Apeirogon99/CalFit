package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.user.UserInfo;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** UserReader 구현체 - 다른 도메인에서 User 정보 조회시 사용 */
@Component
public class UserReaderImpl implements UserReader {

  private final UserRepository userRepository;

  public UserReaderImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserInfo getUser(Long userId) {
    return userRepository.findById(userId).map(this::toUserInfo).orElse(null);
  }

  @Override
  public List<UserInfo> getUsers(List<Long> userIds) {
    return userRepository.findAllById(userIds).stream().map(this::toUserInfo).toList();
  }

  @Override
  public boolean exists(Long userId) {
    return userRepository.existsById(userId);
  }

  private UserInfo toUserInfo(UserEntity entity) {
    return new UserInfo(
        entity.getId(),
        entity.getEmail(),
        entity.getName(),
        entity.getRole().name(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
