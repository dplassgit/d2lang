package com.plasstech.lang.d2.common;

import java.util.Optional;

import com.google.common.base.Preconditions;

public class StatusOr<T> {
  private final Optional<T> value;
  private final Optional<RuntimeException> exception;

  public static <T> StatusOr<T> ok(T value) {
    return new StatusOr<T>(value);
  }

  public static <T> StatusOr<T> error(RuntimeException e) {
    return new StatusOr<T>(e);
  }

  public StatusOr(T value) {
    this.value = Optional.of(value);
    this.exception = Optional.empty();
  }

  public StatusOr(RuntimeException e) {
    this.value = Optional.empty();
    this.exception = Optional.of(e);
  }

  public T value() {
    Preconditions.checkState(value.isPresent(), "Cannot get value for a non-OK status");
    return value.get();
  }

  public boolean isOk() {
    return exception.isEmpty();
  }

  public RuntimeException exception() {
    Preconditions.checkState(exception.isPresent(), "Cannot get exception for an OK status");
    return exception.get();
  }
}
