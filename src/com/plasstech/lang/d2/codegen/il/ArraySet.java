package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.ArrayType;

/** Sets the value of an array slot, i.e., array[index] = source */
public class ArraySet extends Op {
  private final Location array;
  private final Operand index;
  private final Operand source;
  private final ArrayType arrayType;
  private final boolean isArrayLiteral;

  public ArraySet(
      Location array, ArrayType arrayType, Operand index, Operand source, boolean isArrayLiteral,
      Position position) {
    super(position);
    this.arrayType = arrayType;
    this.array = array;
    this.index = index;
    this.source = source;
    this.isArrayLiteral = isArrayLiteral;
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  /** The base array. a[i]=s */
  public Location array() {
    return array;
  }

  public Operand index() {
    return index;
  }

  public Operand source() {
    return source;
  }

  @Override
  public String toString() {
    return String.format("%s[%s] = %s", array, index, source);
  }

  public ArrayType arrayType() {
    return arrayType;
  }

  public boolean isArrayLiteral() {
    return isArrayLiteral;
  }
}
