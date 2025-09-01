package com.plasstech.lang.d2.type;

/**
 * Represents an unbound generic type parameter. For example:
 *
 * <pre>
 *  genrecord: record<T> {f: T}
 * </pre>
 * 
 * The "T" is a formal type of the RecordReferenceType; its type is UnboundType. Similarly, field
 * `f`'s type is an UnboundType with "T" as its name.
 */
public class UnboundType extends SimpleType {
  public UnboundType(String name) {
    super(name, 8);
  }
}
