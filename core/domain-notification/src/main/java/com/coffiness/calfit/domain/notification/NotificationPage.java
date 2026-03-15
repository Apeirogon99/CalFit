package com.coffiness.calfit.domain.notification;

import java.util.List;

public record NotificationPage(List<Notification> contents, boolean hasNext) {}
