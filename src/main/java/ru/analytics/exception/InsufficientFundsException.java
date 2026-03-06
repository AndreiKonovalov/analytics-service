package ru.analytics.exception;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException() {
        super("INSUFFICIENT_FUNDS", "Insufficient funds to complete the transaction");
    }

    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }
}