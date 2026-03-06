package ru.analytics.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResult(
        boolean success,
        String message,
        String transactionId,
        String externalReference,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp,

        BigDecimal newSourceBalance,
        BigDecimal newDestinationBalance
) {

    public static TransferResult success(
            String transactionId,
            String externalReference,
            BigDecimal newSourceBalance,
            BigDecimal newDestinationBalance
    ) {
        return new TransferResult(
                true,
                "Transfer completed successfully",
                transactionId,
                externalReference,
                LocalDateTime.now(),
                newSourceBalance,
                newDestinationBalance
        );
    }

    public static TransferResult failure(String message) {
        return new TransferResult(
                false,
                message,
                null,
                null,
                LocalDateTime.now(),
                null,
                null
        );
    }
}