package com.coffiness.calfit.storage.db.core.calendar;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleAttendeeRepository extends JpaRepository<ScheduleAttendeeEntity, Long> {

  // 특정 일정에 참석된 참석자 데이터 가져오기
  List<ScheduleAttendeeEntity> findByScheduleId(Long scheduleId);

  // 여러 일정에 연결된 참석자 목록을 한 번에 조회
  List<ScheduleAttendeeEntity> findAllByScheduleIdIn(List<Long> scheduleIds);

  // 일정이 삭제될 때 참석자 데이터도 삭제
  // 벌크 연산 처리
  @Modifying
  @Query("DELETE FROM ScheduleAttendeeEntity s WHERE s.scheduleId = :scheduleId")
  void deleteByScheduleId(Long scheduleId);
}
