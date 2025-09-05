package com.plasstech.lang.d2.codegen.x64;

import javax.annotation.Nullable;

import com.google.auto.value.AutoBuilder;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/**
 * An operand that is "resolved" already to either a location or a register.
 */
record ResolvedOperand(Operand operand, String name, @Nullable Location location,
    @Nullable Register register) implements Operand {

  @Override
  public VarType type() {
    return operand().type();
  }

  @Override
  public boolean isConstant() {
    return operand().isConstant();
  }

  @Override
  public boolean isRegister() {
    return register() != null;
  }

  @Override
  public SymbolStorage storage() {
    return operand().storage();
  }

  @Override
  public String toString() {
    return name();
  }

  ResolvedOperand setRegister(Register reg) {
    if (reg == null) {
      return this;
    }
    return toBuilder().setRegister(reg).build();
  }

  static ResolvedOperand create(Operand operand, String name) {
    ResolvedOperand.Builder builder = Builder.builder().setOperand(operand).setName(name);
    if (operand instanceof Location location) {
      // I wish this was easier
      builder.setLocation(location);
    }
    return builder.build();
  }

  Builder toBuilder() {
    return new AutoBuilder_ResolvedOperand_Builder(this);
  }

  @AutoBuilder(ofClass = ResolvedOperand.class)
  abstract static class Builder {
    static Builder builder() {
      return new AutoBuilder_ResolvedOperand_Builder();
    }

    abstract ResolvedOperand.Builder setOperand(Operand operand);

    abstract ResolvedOperand.Builder setLocation(Location location);

    abstract ResolvedOperand.Builder setName(String name);

    abstract ResolvedOperand.Builder setRegister(Register register);

    abstract ResolvedOperand build();
  }
}