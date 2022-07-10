package com.plasstech.lang.d2.testing;

import static com.google.common.truth.Truth.assertAbout;

import javax.annotation.Nullable;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;

/** A Truth extension that works with VarTypes. */
public class VarTypeSubject extends Subject {

  @Nullable private final VarType actual;

  private VarTypeSubject(FailureMetadata metadata, @Nullable VarType actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  public void isArray() {
    check("isArray").that(actual.isArray()).isTrue();
  }

  public void hasArrayBaseType(VarType baseType) {
    isArray();
    ArrayType arrayType = (ArrayType) actual;
    check("hasArrayBaseType").that(arrayType.baseType()).isEqualTo(baseType);
  }

  public void isUnknown() {
    check("isUnknown").that(actual.isUnknown()).isTrue();
  }

  public static VarTypeSubject assertThat(@Nullable VarType actual) {
    return assertAbout(varTypes()).that(actual);
  }

  public static Factory<VarTypeSubject, VarType> varTypes() {
    return VarTypeSubject::new;
  }
}
