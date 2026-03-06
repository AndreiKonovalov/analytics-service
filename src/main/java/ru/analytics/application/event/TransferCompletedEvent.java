package ru.analytics.application.event;

import ru.analytics.application.dto.TransferRequest;

import java.time.LocalDateTime;

public record TransferCompletedEvent(
        String eventType,
        TransferRequest transferRequest,
        String externalReference,
        LocalDateTime operationTime,
        boolean success,
        String errorMessage) {

    public static TransferCompletedEvent success(
            TransferRequest request,
            String externalReference,
            LocalDateTime time
    ) {
        return new TransferCompletedEvent(
                "TRANSFER_COMPLETED",
                request,
                externalReference,
                time,
                true,
                null
        );
    }

    public static TransferCompletedEvent failure(
            TransferRequest request,
            String errorMessage
    ) {
        return new TransferCompletedEvent(
                "TRANSFER_COMPLETED",
                request,
                null,
                LocalDateTime.now(),
                false,
                errorMessage
        );
    }

}
