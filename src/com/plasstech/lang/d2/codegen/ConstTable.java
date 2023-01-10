package com.plasstech.lang.d2.codegen;

import java.util.List;

public interface ConstTable<T> {

  void add(T value);

  ConstEntry<T> lookup(T value);

  List<? extends ConstEntry<T>> entries();
}
