package com.coffiness.calfit.domain.announcementBoard;

import java.time.LocalDateTime;

public record AnnouncementBoard(
    Long id, String title, String content, Boolean pinned, LocalDateTime createdAt) {}
