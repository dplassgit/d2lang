package com.plasstech.lang.d2.phase;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.D2RuntimeException;

public class Errors {
  private final List<D2RuntimeException> errors = new ArrayList<>();
  private final boolean stopOnFirstError;

  public Errors() {
    this(false);
  }

  public Errors(boolean stopOnFirstError) {
    this.stopOnFirstError = stopOnFirstError;
  }

  public void add(D2RuntimeException e) {
    errors.add(e);
    if (errors.size() > 100 || stopOnFirstError) {
      throw e;
    }
  }

  public ImmutableList<D2RuntimeException> errors() {
    return ImmutableList.copyOf(errors);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
