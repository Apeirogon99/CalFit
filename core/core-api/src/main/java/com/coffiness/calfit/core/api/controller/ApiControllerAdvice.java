package com.coffiness.calfit.core.api.controller;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerAdvice {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    log.info("ValidationException : {}", e.getMessage());
    return buildErrorResponse(
        request, ErrorType.VALIDATION_ERROR, ApiResponse.error(ErrorType.VALIDATION_ERROR));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> handleIllegalArgumentException(
      IllegalArgumentException e, HttpServletRequest request) {
    log.info("IllegalArgumentException : {}", e.getMessage());
    return buildErrorResponse(
        request,
        ErrorType.BAD_REQUEST,
        ApiResponse.error(ErrorType.BAD_REQUEST, Map.of("message", e.getMessage())));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<?> handleIllegalStateException(
      IllegalStateException e, HttpServletRequest request) {
    String message = e.getMessage();

    if (message != null && message.startsWith("GOOGLE_")) {
      log.info("GoogleCalendarException : {}", message, e);
      return buildErrorResponse(
          request, ErrorType.BAD_REQUEST, ApiResponse.error(ErrorType.BAD_REQUEST, message, null));
    }

    log.error("IllegalStateException : {}", message, e);
    return buildErrorResponse(
        request, ErrorType.DEFAULT_ERROR, ApiResponse.error(ErrorType.DEFAULT_ERROR));
  }

  @ExceptionHandler(CoreException.class)
  public ResponseEntity<?> handleCoreException(CoreException e, HttpServletRequest request) {
    switch (e.getErrorType().getLogLevel()) {
      case ERROR -> log.error("CoreException : {}", e.getMessage(), e);
      case WARN -> log.warn("CoreException : {}", e.getMessage(), e);
      default -> log.info("CoreException : {}", e.getMessage(), e);
    }
    return buildErrorResponse(
        request,
        e.getErrorType(),
        ApiResponse.error(e.getErrorType(), e.getCustomCode(), e.getData()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
    log.error("Exception : {}", e.getMessage(), e);
    return buildErrorResponse(
        request, ErrorType.DEFAULT_ERROR, ApiResponse.error(ErrorType.DEFAULT_ERROR));
  }

  private ResponseEntity<?> buildErrorResponse(
      HttpServletRequest request, ErrorType errorType, ApiResponse<?> body) {
    if (isSseRequest(request)) {
      return ResponseEntity.status(errorType.getStatus()).build();
    }
    return new ResponseEntity<>(body, errorType.getStatus());
  }

  private boolean isSseRequest(HttpServletRequest request) {
    String accept = request.getHeader(HttpHeaders.ACCEPT);
    return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
  }
}
