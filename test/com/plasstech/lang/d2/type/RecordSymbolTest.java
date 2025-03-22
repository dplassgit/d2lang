package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

public class RecordSymbolTest {

  @Test
  public void isGeneric_noFormals() {
    RecordDeclarationNode node = new RecordDeclarationNode("rec", ImmutableList.of(), null);
    RecordSymbol rs = new RecordSymbol(node);
    assertThat(rs.isGeneric()).isFalse();
  }

  @Test
  public void isGeneric_formals() {
    RecordDeclarationNode node = new RecordDeclarationNode("rec", ImmutableList.of(), null,
        ImmutableList.of("T"));
    RecordSymbol rs = new RecordSymbol(node);
    assertThat(rs.isGeneric()).isTrue();
  }

  @Test
  public void name_unbound() {
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(), null, ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.name()).isEqualTo("rec<T: unbound, U: unbound>");
  }

  @Test
  public void bind_name_noFields() {
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(), null, ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("U", VarType.STRING, "T", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<INT, STRING>");
  }

  @Test
  public void bind_noGenericFields() {
    DeclarationNode field = new DeclarationNode("f1", VarType.INT, null);
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(field), null, ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("U", VarType.STRING, "T", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<INT, STRING>");
    assertThat(unboundSymbol.fieldType("f1")).isEqualTo(VarType.INT);
  }

  @Test
  public void bind_genericField() {
    DeclarationNode f1 = new DeclarationNode("f1", new UnboundType("T"), null);
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(f1), null,
            ImmutableList.of("T"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.fieldType("f1")).isInstanceOf(UnboundType.class);

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
  }

  @Test
  public void bind_repeatedGenericField() {
    DeclarationNode f1 = new DeclarationNode("f1", new UnboundType("T"), null);
    DeclarationNode f2 = new DeclarationNode("f2", new UnboundType("T"), null);
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(f1, f2), null,
            ImmutableList.of("T"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.fieldType("f2")).isEqualTo(VarType.STRING);
  }

  @Test
  public void bind_multipleunGenericFields() {
    DeclarationNode f1 = new DeclarationNode("f1", new UnboundType("T"), null);
    DeclarationNode f2 = new DeclarationNode("f2", new UnboundType("U"), null);
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(f1, f2), null,
            ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING, "U", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING, INT>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.fieldType("f2")).isEqualTo(VarType.INT);
  }

  @Test
  public void bind_boundAndUnboundFields() {
    DeclarationNode f1 = new DeclarationNode("f1", new UnboundType("T"), null);
    DeclarationNode f2 = new DeclarationNode("f2", VarType.STRING, null);
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(f1, f2), null,
            ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING, "U", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING, INT>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.fieldType("f2")).isEqualTo(VarType.STRING);
  }
}
