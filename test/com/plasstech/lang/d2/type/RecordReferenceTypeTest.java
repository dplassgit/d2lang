package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class RecordReferenceTypeTest {
  private static final ImmutableMap<String, VarType> BINDING =
      ImmutableMap.of("S", VarType.STRING, "T", VarType.INT);
  private static final ImmutableList<UnboundType> FORMALS =
      ImmutableList.of(new UnboundType("T"), new UnboundType("S"));

  private static final RecordReferenceType SIMPLE_RECORD = new RecordReferenceType("simple");
  private static final RecordReferenceType UNBOUND_RECORD =
      new RecordReferenceType("simple", FORMALS);
  private static final RecordReferenceType BOUND_RECORD =
      new RecordReferenceType("simple", FORMALS, ImmutableList.of(VarType.INT));

  @Test
  public void simple() {
    assertThat(SIMPLE_RECORD.actualTypes()).isEmpty();
    assertThat(SIMPLE_RECORD.formalTypes()).isEmpty();
    assertThat(SIMPLE_RECORD.isGeneric()).isFalse();
  }

  @Test
  public void unbound() {
    assertThat(UNBOUND_RECORD.actualTypes()).isEmpty();
    assertThat(UNBOUND_RECORD.formalTypes()).isEqualTo(FORMALS);
    assertThat(UNBOUND_RECORD.isGeneric()).isTrue();
  }

  @Test
  public void bound() {
    assertThat(BOUND_RECORD.actualTypes()).containsExactly(VarType.INT);
    assertThat(UNBOUND_RECORD.formalTypes()).isEqualTo(FORMALS);
    assertThat(BOUND_RECORD.isGeneric()).isTrue();
  }

  @Test
  public void bind_nongeneric() {
    assertThrows(IllegalStateException.class, () -> {
      SIMPLE_RECORD.bind(BINDING);
    });
  }

  @Test
  public void bind_bound() {
    assertThrows(IllegalStateException.class, () -> {
      BOUND_RECORD.bind(BINDING);
    });
  }

  @Test
  public void bind_wrongCount() {
    assertThrows(IllegalStateException.class, () -> {
      UNBOUND_RECORD.bind(ImmutableMap.of());
    });
  }

  @Test
  public void bind_unbound() {
    RecordReferenceType newBound = UNBOUND_RECORD.bind(BINDING);
    assertThat(newBound.actualTypes()).containsExactly(VarType.INT, VarType.STRING);
    assertThat(newBound.formalTypes()).isEqualTo(FORMALS);
  }
}
