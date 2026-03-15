package com.coffiness.calfit.domain;

import java.util.List;

public interface ExternalCalendarReader {

  ExternalCalendar read(Long externalCalendarId);

  ExternalCalendar readForUpdate(Long externalCalendarId);

  ExternalCalendar readByUserIdAndCalendarId(Long userId, String calendarId);

  ExternalCalendar readSyncEnabledByUserId(Long userId);

  List<ExternalCalendar> readAllSyncEnabled();
}
