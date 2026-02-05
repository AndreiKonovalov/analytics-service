package ru.analytics.application.event;

import java.time.LocalDateTime;

public interface AnalyticsEvent {

    String eventType();
    LocalDateTime occurredAt();

}

