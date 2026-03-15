package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import java.util.List;

public record MeetingRoomResponse(
    Long id,
    String name,
    Integer location,
    Integer capacity,
    String description,
    List<String> facilities,
    String color) {

  public static MeetingRoomResponse from(MeetingRoom meetingRoom) {
    return new MeetingRoomResponse(
        meetingRoom.id(),
        meetingRoom.name(),
        meetingRoom.location(),
        meetingRoom.capacity(),
        meetingRoom.description(),
        meetingRoom.facilities(),
        meetingRoom.color());
  }
}
