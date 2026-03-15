package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.ExternalCalendar;
import com.coffiness.calfit.domain.ExternalCalendarReader;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarEntity;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalCalendarReaderImpl implements ExternalCalendarReader {

  private final ExternalCalendarRepository externalCalendarRepository;

  @Override
  public ExternalCalendar read(Long externalCalendarId) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatus(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    return toDomain(entity);
  }

  @Override
  public ExternalCalendar readForUpdate(Long externalCalendarId) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatusForUpdate(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    return toDomain(entity);
  }

  @Override
  public ExternalCalendar readByUserIdAndCalendarId(Long userId, String calendarId) {
    return externalCalendarRepository
        .findByUserIdAndCalendarIdAndStatus(userId, calendarId, EntityStatus.ACTIVE)
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  public ExternalCalendar readSyncEnabledByUserId(Long userId) {
    return externalCalendarRepository.findAllByUserIdAndStatus(userId, EntityStatus.ACTIVE).stream()
        .filter(ExternalCalendarEntity::isSyncEnabled)
        .max(Comparator.comparing(ExternalCalendarEntity::getId))
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  public List<ExternalCalendar> readAllSyncEnabled() {
    return externalCalendarRepository
        .findAllByStatusAndIsSyncEnabled(EntityStatus.ACTIVE, true)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  private ExternalCalendar toDomain(ExternalCalendarEntity entity) {
    return new ExternalCalendar(
        entity.getId(),
        entity.getUserId(),
        entity.getCalendarId(),
        entity.getAccessToken(),
        entity.getRefreshToken(),
        entity.getTokenExpiresAt(),
        entity.getSyncToken(),
        entity.isSyncEnabled(),
        entity.getChannelId(),
        entity.getChannelResourceId(),
        entity.getChannelExpiresAt());
  }
}
