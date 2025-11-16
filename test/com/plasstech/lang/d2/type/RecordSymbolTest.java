package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import org.junit.Test;

public class RecordSymbolTest {

  @Test
  public void isGeneric_noFormals() {
    RecordDeclarationNode node = new RecordDeclarationNode("rec", ImmutableList.of(), null);
    RecordSymbol rs = new RecordSymbol(node);
    assertThat(rs.isGeneric()).isFalse();
    assertThat(rs.varType()).isNotNull();
  }

  @Test
  public void isGeneric_formals() {
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(), null, ImmutableList.of("T"));
    RecordSymbol rs = new RecordSymbol(node);
    assertThat(rs.isGeneric()).isTrue();
    assertThat(rs.varType()).isNotNull();
  }

  @Test
  public void name_unbound() {
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(), null, ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.name()).isEqualTo("rec<T: unbound, U: unbound>");
    assertThat(unboundSymbol.varType()).isNotNull();
  }

  @Test
  public void bind_name_noFields() {
    RecordDeclarationNode node =
        new RecordDeclarationNode("rec", ImmutableList.of(), null, ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.varType()).isNotNull();

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("U", VarType.STRING, "T", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<INT, STRING>");
    assertThat(boundSymbol.varType()).isNotNull();
  }

  @Test
  public void bind_noGenericFields() {
    RecordDeclarationNode node =
        new RecordDeclarationNode(
            "rec",
            ImmutableList.of(new DeclarationNode("f1", VarType.INT, null)),
            null,
            ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.fieldType("f1")).isEqualTo(VarType.INT);
    assertThat(unboundSymbol.varType()).isNotNull();

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("U", VarType.STRING, "T", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<INT, STRING>");
    assertThat(boundSymbol.varType()).isNotNull();
  }

  @Test
  public void bind_genericField() {
    RecordDeclarationNode node =
        new RecordDeclarationNode(
            "rec",
            ImmutableList.of(new DeclarationNode("f1", new UnboundType("T"), null)),
            null,
            ImmutableList.of("T"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.fieldType("f1")).isInstanceOf(UnboundType.class);
    assertThat(unboundSymbol.varType()).isNotNull();

    RecordSymbol boundSymbol = unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.varType()).isNotNull();
  }

  @Test
  public void bind_repeatedGenericField() {
    RecordDeclarationNode node =
        new RecordDeclarationNode(
            "rec",
            ImmutableList.of(
                new DeclarationNode("f1", new UnboundType("T"), null),
                new DeclarationNode("f2", new UnboundType("T"), null)),
            null,
            ImmutableList.of("T"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.varType()).isNotNull();

    RecordSymbol boundSymbol = unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.fieldType("f2")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.varType()).isNotNull();
  }

  @Test
  public void bind_multipleunGenericFields() {
    RecordDeclarationNode node =
        new RecordDeclarationNode(
            "rec",
            ImmutableList.of(
                new DeclarationNode("f1", new UnboundType("T"), null),
                new DeclarationNode("f2", new UnboundType("U"), null)),
            null,
            ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.varType()).isNotNull();

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING, "U", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING, INT>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.fieldType("f2")).isEqualTo(VarType.INT);
    assertThat(boundSymbol.varType()).isNotNull();
  }

  @Test
  public void bind_boundAndUnboundFields() {
    RecordDeclarationNode node =
        new RecordDeclarationNode(
            "rec",
            ImmutableList.of(
                new DeclarationNode("f1", new UnboundType("T"), null),
                new DeclarationNode("f2", VarType.STRING, null)),
            null,
            ImmutableList.of("T", "U"));
    RecordSymbol unboundSymbol = new RecordSymbol(node);
    assertThat(unboundSymbol.varType()).isNotNull();

    RecordSymbol boundSymbol =
        unboundSymbol.bind(ImmutableMap.of("T", VarType.STRING, "U", VarType.INT));
    assertThat(boundSymbol.name()).isEqualTo("rec<STRING, INT>");
    assertThat(boundSymbol.fieldType("f1")).isEqualTo(VarType.STRING);
    assertThat(boundSymbol.fieldType("f2")).isEqualTo(VarType.STRING);
    VarType varType = boundSymbol.varType();
    assertThat(varType).isNotNull();
    assertThat(varType).isInstanceOf(RecordReferenceType.class);
    RecordReferenceType rrt = (RecordReferenceType) varType;
    assertThat(rrt.actualTypes()).containsExactly(VarType.STRING, VarType.INT);
    assertThat(rrt.formalTypes()).isNotEmpty();
  }
}
