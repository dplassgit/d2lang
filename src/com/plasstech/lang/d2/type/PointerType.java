package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.common.Target;

/** A VarType whose size comes from {@link Target}'s pointer size. */
abstract class PointerType extends SimpleType {
  PointerType(String name) {
    super(name, Target.target().pointerSize);
  }
}
