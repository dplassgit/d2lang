package com.plasstech.lang.d2.codegen.x64;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

@AutoValue
abstract class ResolvedOperand implements Operand {
  abstract Operand operand();

  @Nullable
  abstract Location location();

  abstract String name();

  @Nullable
  abstract Register register();

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

  public ResolvedOperand setRegister(Register reg) {
    if (reg == null) {
      return this;
    }
    return this.toBuilder().setRegister(reg).build();
  }

  public static ResolvedOperand create(Operand operand, String name) {
    ResolvedOperand.Builder builder = new AutoValue_ResolvedOperand.Builder().setOperand(operand)
        .setName(name);
    if (operand instanceof Location) {
      // I wish this was easier
      builder.setLocation((Location) operand);
    }
    return builder.build();
  }

  public abstract ResolvedOperand.Builder toBuilder();

  @AutoValue.Builder
  abstract static class Builder {
    abstract ResolvedOperand.Builder setOperand(Operand operand);

    abstract ResolvedOperand.Builder setLocation(Location location);

    abstract ResolvedOperand.Builder setName(String name);

    abstract ResolvedOperand.Builder setRegister(Register register);

    abstract ResolvedOperand build();
  }
}