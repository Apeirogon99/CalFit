package com.coffiness.calfit.domain.meetingRoom;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

  private final MeetingRoomReader meetingRoomReader;
  private final MeetingRoomStore meetingRoomStore;
  private final MeetingRoomValidator meetingRoomValidator;

  /* 회의실 생성 */
  @Transactional
  public MeetingRoom create(
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId) {
    meetingRoomValidator.validateCreateInput(
        name, location, capacity, description, facilities, color, userId);
    return meetingRoomStore.create(
        name, location, capacity, description, facilities, color, userId);
  }

  /* 회의실 수정 */
  @Transactional
  public MeetingRoom update(
      Long id,
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId) {
    meetingRoomValidator.validateUpdateInput(
        id, name, location, capacity, description, facilities, color, userId);
    return meetingRoomStore.update(
        id, name, location, capacity, description, facilities, color, userId);
  }

  /* 회의실 삭제 */
  @Transactional
  public void delete(Long id, Long userId) {
    meetingRoomValidator.validateDeleteRequest(id, userId);
    meetingRoomStore.delete(id, userId);
  }

  /* 회의실 리스트 조회 */
  @Transactional(readOnly = true)
  public List<MeetingRoom> list() {
    return meetingRoomReader.getMeetingRooms();
  }
}
