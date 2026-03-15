package com.coffiness.calfit.core.support.response;

import com.coffiness.calfit.support.error.ErrorMessage;
import com.coffiness.calfit.support.error.ErrorType;

public class ApiResponse<S> {

  private final ResultType result;

  private final S data;

  private final ErrorMessage error;

  private ApiResponse(ResultType result, S data, ErrorMessage error) {
    this.result = result;
    this.data = data;
    this.error = error;
  }

  public static ApiResponse<?> success() {
    return new ApiResponse<>(ResultType.SUCCESS, null, null);
  }

  public static <S> ApiResponse<S> success(S data) {
    return new ApiResponse<>(ResultType.SUCCESS, data, null);
  }

  public static ApiResponse<?> error(ErrorType error) {
    return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error));
  }

  public static ApiResponse<?> error(ErrorType error, Object errorData) {
    return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error, errorData));
  }

  public static ApiResponse<?> error(ErrorType error, String customCode, Object errorData) {
    return new ApiResponse<>(
        ResultType.ERROR, null, new ErrorMessage(error, customCode, errorData));
  }

  public ResultType getResult() {
    return result;
  }

  public S getData() {
    return data;
  }

  public ErrorMessage getError() {
    return error;
  }
}
