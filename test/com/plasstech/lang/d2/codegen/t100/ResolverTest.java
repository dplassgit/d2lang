package com.plasstech.lang.d2.codegen.t100;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.type.VarType;

public class ResolverTest {

  private Emitter emitter = new T100Emitter();
  private Resolver resolver = new Resolver(null, null, emitter);

  private static final TempLocation TEMP_BOOL =
      LocationUtils.newTempLocation("__tempboo", VarType.BOOL);
  private static final TempLocation TEMP_BYTE =
      LocationUtils.newTempLocation("__tempb", VarType.BYTE);
  private static final TempLocation TEMP_BYTE2 =
      LocationUtils.newTempLocation("__tempb2", VarType.BYTE);
  private static final TempLocation TEMP_INT =
      LocationUtils.newTempLocation("__tempi", VarType.INT);

  @Test
  public void resolve_bool_constants() {
    String actual = resolver.resolve(ConstantOperand.TRUE);
    assertThat(actual).isEqualTo("0x01");
    actual = resolver.resolve(ConstantOperand.FALSE);
    assertThat(actual).isEqualTo("0x00");
  }

  @Test
  public void resolve_byte_constant() {
    String actual = resolver.resolve(ConstantOperand.of((byte) 0x34));
    assertThat(actual).isEqualTo("0x34");
  }

  @Test
  public void resolve_16BitConstant() {
    String actual = resolver.resolve(ConstantOperand.of(0x1234));
    assertThat(actual).isEqualTo("0x1234");
  }

  @Test
  public void resolve_temp() {
    String actual = resolver.resolve(TEMP_BYTE);
    assertThat(actual).isEqualTo("_TEMP_BYTE_0");
  }

  @Test
  public void deallocate_then_resolve_reuses_pseudoreg() {
    resolver.resolve(TEMP_BYTE);
    resolver.deallocate(TEMP_BYTE);

    // now allocate another one; it should use the same location
    String actual = resolver.resolve(TEMP_BYTE2);
    assertThat(actual).isEqualTo("_TEMP_BYTE_0");
  }

  @Test
  public void deallocate_then_resolve_different_type_makes_new_pseudoreg() {
    resolver.resolve(TEMP_BYTE);
    resolver.deallocate(TEMP_BYTE);

    String actual = resolver.resolve(TEMP_BOOL);
    assertThat(actual).isEqualTo("_TEMP_BOOL_0");
  }

  @Test
  public void resolve_different_types() {
    String byteTempLocation = resolver.resolve(TEMP_BYTE);
    String intTempLocation = resolver.resolve(TEMP_INT);
    assertThat(intTempLocation).isEqualTo("_TEMP_INT_0");
    assertThat(byteTempLocation).isNotEqualTo(intTempLocation);
  }
}
