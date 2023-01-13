package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.common.Target;

abstract class PointerType extends SimpleType {
  PointerType(String name) {
    super(name, Target.target().pointerSize);
  }
}
