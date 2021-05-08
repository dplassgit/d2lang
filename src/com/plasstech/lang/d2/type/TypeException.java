package com.plasstech.lang.d2.type;

public class TypeException extends RuntimeException {
  private final String error;

  public TypeException(String error) {
    this.error = error;
  }

  public String error() {
    return error;
  }

}
