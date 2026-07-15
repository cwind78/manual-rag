package com.prj.manualrag.common.advice;

import com.prj.manualrag.common.dto.ApiResponse;
import com.prj.manualrag.common.dto.ErrorResponse;
import com.prj.manualrag.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validationException(
            MethodArgumentNotValidException e
    ) {

        FieldError fieldError =
                e.getBindingResult()
                        .getFieldError();

        String message =
                fieldError == null
                        ? "잘못된 요청입니다."
                        : fieldError.getDefaultMessage();

        log.warn("Validation Error : {}", message);

        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.fail(
                                new ErrorResponse(
                                        ErrorCode.VALIDATION_ERROR.name(),
                                        message,
                                        LocalDateTime.now()
                                )
                        )
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> illegalArgumentException(
            IllegalArgumentException e
    ) {

        log.warn("IllegalArgument : {}", e.getMessage());

        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.fail(
                                new ErrorResponse(
                                        ErrorCode.INVALID_REQUEST.name(),
                                        e.getMessage(),
                                        LocalDateTime.now()
                                )
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> exception(
            Exception e
    ) {

        log.error("Unexpected Exception", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.fail(
                                new ErrorResponse(
                                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                                        "서버 오류가 발생했습니다.",
                                        LocalDateTime.now()
                                )
                        )
                );
    }

}
