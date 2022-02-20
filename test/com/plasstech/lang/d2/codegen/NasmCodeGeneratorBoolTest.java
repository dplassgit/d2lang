package com.plasstech.lang.d2.codegen;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorBoolTest extends NasmCodeGeneratorTestBase {
  @Test
  public void boolAssign(@TestParameter boolean bool) throws Exception {
    execute(String.format("a=%s b=a print a print b", bool), "boolAssign" + bool);
  }

  @Test
  public void not(@TestParameter boolean bool) throws Exception {
    execute(String.format("a=%s c=not a print a print c", bool), "not" + bool);
  }

  @Test
  public void boolBinOp(
      @TestParameter({"and", "or", "xor", "<", "<=", "==", "!=", ">", ">="}) String op,
      @TestParameter boolean boola,
      @TestParameter boolean boolb)
      throws Exception {
    execute(
        String.format("a=%s b=%s c=a %s b print c d=b %s a print d", boola, boolb, op, op),
        "boolBinOp" + boola + boolb);
  }
}
