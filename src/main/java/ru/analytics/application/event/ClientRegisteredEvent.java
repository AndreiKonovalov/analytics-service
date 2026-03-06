package ru.analytics.application.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ClientRegisteredEvent(
        String eventType,
        Long clientId,
        String email,
        String fullName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDateTime registeredAt
) {
    public static ClientRegisteredEvent of(
            Long clientId,
            String email,
            String fullName
    ) {
        return new ClientRegisteredEvent(
                "CLIENT_REGISTERED",
                clientId,
                email,
                fullName,
                LocalDateTime.now()
        );
    }

}
