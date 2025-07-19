package com.repository.tracker.RepoTracker.exceptions;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConnectorException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConnector(ConnectorException ex) {
        ErrorResponse err = new ErrorResponse(
                "EXTERNAL_API_ERROR",
                ex.getMessage()
        );
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(err));
    }

    @AllArgsConstructor
    public static class ErrorResponse {
        String code;
        String message;
    }
}
