package com.coffiness.calfit.support.error;

public class ErrorMessage {

  private final String code;

  private final String message;

  private final String customCode;

  private final Object data;

  protected ErrorMessage() {
    this.code = null;
    this.message = null;
    this.customCode = null;
    this.data = null;
  }

  public ErrorMessage(ErrorType errorType) {
    this.code = errorType.getCode().name();
    this.message = errorType.getMessage();
    this.customCode = null;
    this.data = null;
  }

  public ErrorMessage(ErrorType errorType, Object data) {
    this.code = errorType.getCode().name();
    this.message = errorType.getMessage();
    this.customCode = null;
    this.data = data;
  }

  public ErrorMessage(ErrorType errorType, String customCode, Object data) {
    this.code = errorType.getCode().name();
    this.message = errorType.getMessage();
    this.customCode = (customCode != null && !customCode.isBlank()) ? customCode : null;
    this.data = data;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public String getCustomCode() {
    return customCode;
  }

  public Object getData() {
    return data;
  }
}
