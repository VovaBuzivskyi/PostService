package faang.school.postservice.exception.handler;

import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.exception.FeignClientException;
import faang.school.postservice.exception.FileProcessException;
import faang.school.postservice.exception.ForbiddenException;
import faang.school.postservice.exception.PostException;
import faang.school.postservice.exception.UnauthorizedException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({DataValidationException.class, PostException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDataValidationExceptionAndPostException(Exception e) {
        log.error("Exception handled: {}", e.getClass().getSimpleName(), e);
        return buildResponse(e);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEntityNotFoundException(EntityNotFoundException e) {
        log.error("EntityNotFoundException", e);
        return buildResponse(e);
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorizedException(UnauthorizedException e) {
        log.error("UnauthorizedException", e);
        return buildResponse(e);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbiddenException(ForbiddenException e) {
        log.error("ForbiddenException", e);
        return buildResponse(e);
    }

    @ExceptionHandler(FeignClientException.class)
    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    public ErrorResponse handleFeignClientException(FeignClientException e) {
        log.error("FeignClientException", e);
        return buildResponse(e);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception e) {
        log.error("Exception", e);
        return buildResponse(e);
    }

    @ExceptionHandler(FileProcessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(FileProcessException e) {
        log.error("FileProcessException", e);
        return buildResponse(e);
    }

    private ErrorResponse buildResponse(Exception e) {
        log.error(e.getClass().getSimpleName(), e);
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .error(e.getClass().getName())
                .message(e.getMessage())
                .build();
    }
}
