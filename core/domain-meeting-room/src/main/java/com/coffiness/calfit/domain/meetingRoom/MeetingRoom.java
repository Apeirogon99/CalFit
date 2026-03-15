package com.coffiness.calfit.domain.meetingRoom;

import java.util.List;

public record MeetingRoom(
    Long id,
    String name,
    Integer location,
    Integer capacity,
    String description,
    List<String> facilities,
    String color) {}
