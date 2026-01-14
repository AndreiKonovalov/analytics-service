package ru.analytics.application.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.analytics.application.dto.TransferRequest;

import java.time.LocalDateTime;

@Getter
public class TransferCompletedEvent extends ApplicationEvent {

    private final TransferRequest transferRequest;
    private final String externalReference;
    private final LocalDateTime operationTime;
    private final boolean success;
    private final String errorMessage;

    public TransferCompletedEvent(Object source,
                                  TransferRequest transferRequest,
                                  String externalReference,
                                  LocalDateTime operationTime) {
        super(source);
        this.transferRequest = transferRequest;
        this.externalReference = externalReference;
        this.operationTime = operationTime;
        this.success = true;
        this.errorMessage = null;
    }

    public TransferCompletedEvent(Object source,
                                  TransferRequest transferRequest,
                                  String errorMessage) {
        super(source);
        this.transferRequest = transferRequest;
        this.externalReference = null;
        this.operationTime = LocalDateTime.now();
        this.success = false;
        this.errorMessage = errorMessage;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    // Метод для совместимости, если нужно время в миллисекундах
    public long getOperationTimeMillis() {
        return operationTime.atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}
