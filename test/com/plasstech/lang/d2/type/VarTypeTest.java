package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class VarTypeTest {
  @Test
  public void fromName(
      @TestParameter(
        {"BYTE", "INT", "LONG", "DOUBLE", "BOOL", "STRING", "VOID", "UNKNOWN", "NULL"}
      ) String name) {
    assertThat(VarType.fromName(name)).isNotNull();
  }

  @Test
  public void fromName_bad(@TestParameter({"byte", "", "xLONG", "ARRAY"}) String name) {
    assertThat(VarType.fromName(name)).isNull();
  }

  @Test
  public void newArrayType() {
    ArrayType at = new ArrayType(VarType.INT, 1);
    assertThat(VarType.fromName(at.name())).isEqualTo(at);
  }

  @Test
  public void newRecordType() {
    RecordReferenceType rrt = new RecordReferenceType("record");
    assertThat(VarType.fromName(rrt.name())).isEqualTo(rrt);
  }
}
