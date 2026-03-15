package com.coffiness.calfit.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ErrorType {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, ErrorCode.E400, "Invalid request.", LogLevel.INFO),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E400, "Bad request.", LogLevel.INFO),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, ErrorCode.E401, "Unauthorized.", LogLevel.INFO),
  FORBIDDEN(HttpStatus.FORBIDDEN, ErrorCode.E403, "Forbidden.", LogLevel.INFO),
  NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404, "Resource not found.", LogLevel.INFO),
  DEFAULT_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR,
      ErrorCode.E500,
      "An unexpected error has occurred.",
      LogLevel.ERROR);

  private final HttpStatus status;

  private final ErrorCode code;

  private final String message;

  private final LogLevel logLevel;

  ErrorType(HttpStatus status, ErrorCode code, String message, LogLevel logLevel) {

    this.status = status;
    this.code = code;
    this.message = message;
    this.logLevel = logLevel;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public ErrorCode getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public LogLevel getLogLevel() {
    return logLevel;
  }
}
