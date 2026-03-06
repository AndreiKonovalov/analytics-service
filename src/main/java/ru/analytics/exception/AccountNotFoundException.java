package ru.analytics.exception;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(Long accountId) {
        super("ACCOUNT_NOT_FOUND",
                String.format("Account with ID %d not found", accountId));
    }

    public AccountNotFoundException(String accountNumber) {
        super("ACCOUNT_NOT_FOUND",
                String.format("Account with number %s not found", accountNumber));
    }
}