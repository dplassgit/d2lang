package com.plasstech.lang.d2.type;

import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.DefaultVisitor;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

/** 
 * Finds record definitions in the parse tree and adds them to the given symbol table. 
 * This is useful for forward referneces to record types.
 */
class RecordGatherer extends DefaultVisitor {
  private final SymTab symbolTable;

  public RecordGatherer(SymTab symbolTable) {
    this.symbolTable = symbolTable;
  }

  @Override
  public void visit(RecordDeclarationNode node) {
    // 1. Make sure no nested records or procs
    for (DeclarationNode field : node.fields()) {
      if (field instanceof RecordDeclarationNode) {
        RecordDeclarationNode subRecord = (RecordDeclarationNode) field;
        throw new TypeException(
            String.format(
                "Cannot declare nested RECORD '%s' in RECORD '%s'", subRecord.name(), node.name()),
            field.position());
      } else if (field instanceof ProcedureNode) {
        ProcedureNode proc = (ProcedureNode) field;
        throw new TypeException(
            String.format(
                "Cannot declare nested PROC '%s' in RECORD '%s'", proc.name(), node.name()),
            field.position());
      }
    }

    // 2. Make sure no duplicated field names
    // Note, NOT immutable list
    List<String> fieldNames = node.fields().stream().map(DeclarationNode::name).collect(toList());
    Set<String> duplicates = new HashSet<>();
    Set<String> uniques = new HashSet<>();
    for (String fieldName : fieldNames) {
      if (uniques.contains(fieldName)) {
        uniques.remove(fieldName);
        duplicates.add(fieldName);
      } else {
        uniques.add(fieldName);
      }
    }
    if (!duplicates.isEmpty()) {
      throw new TypeException(
          String.format(
              "Duplicate field(s) '%s' declared in RECORD '%s'",
              duplicates.toString(), node.name()),
          node.position());
    }

    // Add this record to the symbol table
    symbolTable.declareRecord(node);
  }
}