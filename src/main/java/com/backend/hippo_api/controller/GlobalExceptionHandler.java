package com.backend.hippo_api.controller;

import com.backend.hippo_api.infrastructure.exceptions.ConflictException;
import com.backend.hippo_api.infrastructure.exceptions.ResourceNotFoundException;
import com.backend.hippo_api.infrastructure.records.ErrorResponseRecord;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Tratar uma ConflicException
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseRecord> handleConflictException(ConflictException e) {

        ErrorResponseRecord erro = new ErrorResponseRecord(
                HttpStatus.CONFLICT.value(),
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(erro);
    }

    // Tratar uma BadCredentialsException
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseRecord> handleBadCredentialsException(BadCredentialsException e) {

        ErrorResponseRecord erro = new ErrorResponseRecord(
                HttpStatus.UNAUTHORIZED.value(),
                "E-mail ou Senha Inválidos",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(erro);
    }

    // Tratar uma ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseRecord> handleResourceNotFoundException(ResourceNotFoundException e) {

        ErrorResponseRecord erro = new ErrorResponseRecord(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(erro);
    }

    // Tratar uma MethodArgumentNotValidException
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseRecord> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponseRecord erro = new ErrorResponseRecord(
                HttpStatus.BAD_REQUEST.value(),
                mensagem,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(erro);
    }}
