package com.plasstech.lang.d2.codegen;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class PrintCodeGeneratorTest {

  @Test
  public void format(@TestParameter PrintCodeGenerator.Format fmt, @TestParameter boolean println) {
    System.out.println(fmt.constData(println));
  }
}
