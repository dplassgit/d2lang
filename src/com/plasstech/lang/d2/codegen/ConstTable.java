package com.plasstech.lang.d2.codegen;

import java.util.List;

abstract class ConstTable<T> {

  abstract void add(T value);

  abstract ConstEntry<T> lookup(T value);

  abstract List<? extends ConstEntry<T>> entries();
}
