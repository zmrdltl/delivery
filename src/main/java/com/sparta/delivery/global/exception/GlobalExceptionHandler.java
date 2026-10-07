package com.sparta.delivery.global.exception;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@NullMarked
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        String message = switch (exception) {
            case ResponseStatusException e -> Objects.requireNonNullElse(
                e.getReason(),
                "요청을 처리할 수 없습니다."
            );
            case MethodArgumentNotValidException e -> {
                String validationMessage = e.getBindingResult().getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining(", "));

                yield validationMessage.isBlank()
                    ? "요청 값이 올바르지 않습니다."
                    : validationMessage;
            }
            case HttpMessageNotReadableException ignored ->
                "요청 본문의 형식이나 값이 올바르지 않습니다.";
            default -> {
                HttpStatus httpStatus = HttpStatus.resolve(status.value());
                yield httpStatus != null
                    ? httpStatus.getReasonPhrase()
                    : "요청을 처리할 수 없습니다.";
            }
        };

        ErrorResponse response = new ErrorResponse(status.value(), message);

        return super.handleExceptionInternal(
            exception,
            response,
            headers,
            status,
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
        Exception exception
    ) {
        logger.error("처리하지 못한 요청 오류가 발생했습니다.", exception);

        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "서버 오류가 발생했습니다."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
