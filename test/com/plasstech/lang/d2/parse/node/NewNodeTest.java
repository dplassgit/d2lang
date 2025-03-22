package com.plasstech.lang.d2.parse.node;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.type.VarType;

public class NewNodeTest {

  @Test
  public void fullyQualifiedNameNotGeneric() {
    NewNode node = new NewNode("rec", null);
    assertThat(node.baseRecordName()).isEqualTo("rec");
    assertThat(node.fullyQualifiedRecordName()).isEqualTo(node.baseRecordName());
  }

  @Test
  public void fullyQualifiedNameGeneric() {
    NewNode node = new NewNode("rec", ImmutableList.of(VarType.BOOL, VarType.INT), null);
    assertThat(node.baseRecordName()).isEqualTo("rec");
    assertThat(node.fullyQualifiedRecordName()).isEqualTo("rec<BOOL, INT>");
  }
}
