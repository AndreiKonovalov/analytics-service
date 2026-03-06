package ru.analytics.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(Long id) {
        super("Клиент с ID " + id + " не найден");
    }

    public ClientNotFoundException(String message) {
        super(message);
    }
}