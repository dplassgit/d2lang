package com.plasstech.lang.d2.type.testing;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider;
import com.plasstech.lang.d2.type.VarType;

public class NumericTypeProvider extends TestParameterValuesProvider {
  @Override
  public List<VarType> provideValues(Context context) {
    return ImmutableList.of(VarType.INT, VarType.BYTE, VarType.LONG, VarType.DOUBLE);
  }
}