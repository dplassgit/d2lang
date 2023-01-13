package com.plasstech.lang.d2.codegen.t100;

import static com.google.common.truth.Truth.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.common.Target;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class T100LocationsTest {

  private Target oldTarget;

  @Before
  public void setUp() {
    oldTarget = Target.target();
    Target.setTarget(Target.t100);
  }

  @After
  public void tearDown() {
    Target.setTarget(oldTarget);
  }

  @Test
  public void zeros_int() {
    String zeros = T100Locations.zeros(VarType.INT);
    assertThat(zeros).isEqualTo("0x00,0x00,0x00,0x00");
  }

  @Test
  public void zeros_string() {
    String zeros = T100Locations.zeros(VarType.STRING);
    // two bytes
    assertThat(zeros).isEqualTo("0x00,0x00");
  }

  @Test
  public void zeros_bool() {
    String zeros = T100Locations.zeros(VarType.BOOL);
    assertThat(zeros).isEqualTo("0x00");
  }
}
