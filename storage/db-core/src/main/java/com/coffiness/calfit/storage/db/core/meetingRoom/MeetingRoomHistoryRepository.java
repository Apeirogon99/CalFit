package com.coffiness.calfit.storage.db.core.meetingRoom;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomHistoryRepository
    extends JpaRepository<MeetingRoomHistoryEntity, Long> {}
