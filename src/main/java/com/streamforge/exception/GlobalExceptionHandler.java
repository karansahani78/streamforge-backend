package com.streamforge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> handleEmailAlreadyExist(
            EmailAlreadyExistsException ex
    ) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "timeStamp", LocalDate.now(),
                                "message", ex.getMessage(),
                                "status", HttpStatus.BAD_REQUEST.value()
                        )
                );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(
            BadCredentialsException ex
    ) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        Map.of(
                                "timeStamp", LocalDate.now(),
                                "message", "Invalid email or password",
                                "status", HttpStatus.UNAUTHORIZED.value()
                        )
                );
    }
}