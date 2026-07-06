package com.resume.common;

import com.resume.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AppProperties appProperties;

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusiness(BusinessException ex) {
        return Result.fail(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(Exception ex) {
        return Result.fail(ErrorCode.BAD_REQUEST, "invalid request parameters");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        if (appProperties.isExposeErrorDetails()) {
            return Result.fail(ErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
        return Result.fail(ErrorCode.INTERNAL_ERROR, "internal server error");
    }
}
