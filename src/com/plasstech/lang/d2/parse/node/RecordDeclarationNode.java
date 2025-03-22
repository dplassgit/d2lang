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
  private final String baseName;

  public static String fqName(String baseName, List<String> formalTypeVariables) {
    if (formalTypeVariables.size() == 0) {
      return baseName;
    }
    return String.format("%s<%s>", baseName,
        Joiner.on(", ").join(formalTypeVariables.stream().map(v -> v + ": unbound").toList()));
  }

  /**
   * @param baseName the name of the record
   */
  public RecordDeclarationNode(String baseName, List<DeclarationNode> fields, Position start) {
    this(baseName, fields, start, ImmutableList.of());
  }

  /**
   * @param baseName the name of the record
   * @param formalTypeVariables zero or more type variables (for generic records)
   */
  public RecordDeclarationNode(String baseName, List<DeclarationNode> fields, Position start,
      List<String> formalTypeVariables) {
    // Technically this node doesn't have a type because it's not a referenceable *variable*
    super(fqName(baseName, formalTypeVariables), new RecordReferenceType(baseName), start);
    this.baseName = baseName;
    this.fields = fields;
    this.formalTypeVariables = ImmutableList.copyOf(formalTypeVariables);
  }

  /** The fields declared in this record definition. */
  public List<DeclarationNode> fields() {
    return fields;
  }

  public String baseName() {
    return baseName;
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
}
