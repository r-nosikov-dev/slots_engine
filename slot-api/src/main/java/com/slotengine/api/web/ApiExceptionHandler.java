package com.slotengine.api.web;

import com.slotengine.api.dto.ApiDtos;
import com.slotengine.api.ledger.InsufficientFundsException;
import com.slotengine.api.ledger.WalletException;
import com.slotengine.api.session.SessionStore;
import com.slotengine.model.GameValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SessionStore.SessionNotFoundException.class)
    ResponseEntity<ApiDtos.ErrorResponse> sessionNotFound(SessionStore.SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiDtos.ErrorResponse("SESSION_NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ResponseEntity<ApiDtos.ErrorResponse> broke(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiDtos.ErrorResponse("INSUFFICIENT_FUNDS", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ForbiddenInLiveException.class)
    ResponseEntity<ApiDtos.ErrorResponse> forbidden(ForbiddenInLiveException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiDtos.ErrorResponse(ex.code(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(WalletException.class)
    ResponseEntity<ApiDtos.ErrorResponse> wallet(WalletException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiDtos.ErrorResponse("WALLET_ERROR", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(GameValidationException.class)
    ResponseEntity<ApiDtos.ErrorResponse> invalidGame(GameValidationException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiDtos.ErrorResponse("INVALID_GAME", ex.getMessage(), ex.errors()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiDtos.ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiDtos.ErrorResponse("BAD_REQUEST", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiDtos.ErrorResponse> misconfigured(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiDtos.ErrorResponse("MISCONFIGURED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiDtos.ErrorResponse> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiDtos.ErrorResponse("VALIDATION", "Invalid request", details));
    }
}
