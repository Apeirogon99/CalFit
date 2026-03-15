package com.coffiness.calfit.core.enums;

import lombok.Getter;

@Getter
public enum CareerType {
  NEW("신입"),
  EXPERIENCED("경력"),
  IRRELEVANT("무관");

  private final String description;

  private CareerType(final String description) {
    this.description = description;
  }
}
