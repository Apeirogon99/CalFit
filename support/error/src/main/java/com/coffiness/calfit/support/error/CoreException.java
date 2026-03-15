package com.coffiness.calfit.support.error;

public class CoreException extends RuntimeException {

  private final ErrorType errorType;

  private final String customCode;

  private final Object data;

  public CoreException(ErrorType errorType) {
    super(errorType.getMessage());
    this.errorType = errorType;
    this.customCode = null;
    this.data = null;
  }

  public CoreException(ErrorType errorType, Object data) {
    super(errorType.getMessage());
    this.errorType = errorType;
    this.customCode = null;
    this.data = data;
  }

  public CoreException(ErrorType errorType, String customCode) {
    super(errorType.getMessage());
    this.errorType = errorType;
    this.customCode = customCode;
    this.data = null;
  }

  public CoreException(ErrorType errorType, String customCode, Object data) {
    super(errorType.getMessage());
    this.errorType = errorType;
    this.customCode = customCode;
    this.data = data;
  }

  public ErrorType getErrorType() {
    return errorType;
  }

  public String getCustomCode() {
    return customCode;
  }

  public Object getData() {
    return data;
  }
}
