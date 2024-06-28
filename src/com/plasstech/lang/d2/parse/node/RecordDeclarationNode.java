package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordReferenceType;

/** A parse node for a record definition declaration. */
public class RecordDeclarationNode extends DeclarationNode {

  private final List<DeclarationNode> fields;
  private final ImmutableList<String> formalTypeVariables;

  /**
   * @param name the name of the record
   */
  public RecordDeclarationNode(String name, List<DeclarationNode> fields, Position start) {
    this(name, fields, start, ImmutableList.of());
  }

  /**
   * @param name the name of the record
   * @param formalTypeVariables zero or more type variables (for generic records)
   */
  public RecordDeclarationNode(String name, List<DeclarationNode> fields, Position start,
      List<String> formalTypeVariables) {
    // Technically this node doesn't have a type because it's not a referenceable *variable*
    super(name, new RecordReferenceType(name), start);
    this.fields = fields;
    this.formalTypeVariables = ImmutableList.copyOf(formalTypeVariables);
  }

  /** The fields declared in this record definition. */
  public List<DeclarationNode> fields() {
    return fields;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("%s: RECORD%s{%s}", name(),
        (formalTypeVariables != null && formalTypeVariables.size() > 0)
            ? String.format("<%s>", Joiner.on(", ").join(formalTypeVariables))
            : "",
        fields());
  }

  /**
   * If not empty, the formal type variables for this possibly generic type. For example:
   *
   * <pre>
   * r: record<S, T> {}
   * </pre>
   *
   * it will be S and T.
   */
  public ImmutableList<String> formalTypeVariables() {
    return formalTypeVariables;
  }
}
