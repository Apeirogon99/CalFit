package com.coffiness.calfit.core.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record OffsetLimit(int offset, int limit) {

  public Pageable toPageable() {
    return PageRequest.of(offset / limit, limit);
  }
}
