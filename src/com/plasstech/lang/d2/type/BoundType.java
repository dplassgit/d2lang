package com.plasstech.lang.d2.type;

/**
 * Represents a bound generic type parameter whose type is not known. For example:
 *
 * <pre>
 *  list: record<T> { value: T next: list<T> }
 *  genproc: proc<U>(param: list<U>): U { return param.value }
 * </pre>
 *
 * The "T" is an UnboundType, and the "U" is the BoundType.
 */
public class BoundType extends SimpleType {
  public BoundType(String name) {
    super(name, 8);
  }
}
