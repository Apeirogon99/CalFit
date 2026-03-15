package com.coffiness.calfit.core.api.config;

import com.coffiness.calfit.support.error.CoreException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void handleUncaughtException(Throwable e, Method method, Object... params) {
    if (e instanceof CoreException) {
      switch (((CoreException) e).getErrorType().getLogLevel()) {
        case ERROR -> log.error("CoreException : {}", e.getMessage(), e);
        case WARN -> log.warn("CoreException : {}", e.getMessage(), e);
        default -> log.info("CoreException : {}", e.getMessage(), e);
      }
    } else {
      log.error("Exception : {}", e.getMessage(), e);
    }
  }
}
