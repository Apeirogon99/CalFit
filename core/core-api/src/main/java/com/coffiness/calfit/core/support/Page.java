package com.coffiness.calfit.core.support;

import java.util.List;

public record Page<T>(List<T> contents, Boolean hasNext) {}
