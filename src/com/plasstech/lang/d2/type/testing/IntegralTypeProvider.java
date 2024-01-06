package com.plasstech.lang.d2.type.testing;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter.TestParameterValuesProvider;
import com.plasstech.lang.d2.type.VarType;

public class IntegralTypeProvider implements TestParameterValuesProvider {
  @Override
  public List<VarType> provideValues() {
    return ImmutableList.of(VarType.INT, VarType.BYTE, VarType.LONG);
  }
}