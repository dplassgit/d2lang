package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

class DoubleCodeGenerator extends DefaultOpcodeVisitor {
  private static final Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.PLUS, "addsd")
          .put(TokenType.MINUS, "subsd")
          .put(TokenType.MULT, "mulsd")
          .put(TokenType.DIV, "divsd")
          .put(TokenType.EQEQ, "setz")
          .put(TokenType.NEQ, "setnz")
          .put(TokenType.GT, "seta")
          .put(TokenType.GEQ, "setae")
          .put(TokenType.LT, "setb")
          .put(TokenType.LEQ, "setbe")
          .build();

  private final Resolver resolver;
  private final Emitter emitter;

  public DoubleCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  @Override
  public void visit(BinOp op) {
    VarType leftType = op.left().type();
    if (leftType != VarType.DOUBLE) {
      return;
    }
    ResolvedOperand dest = resolver.resolveFully(op.destination());

    TokenType operator = op.operator();
    switch (operator) {
      case PLUS:
      case MINUS:
      case MULT:
      case DIV:
        resolver.mov(op.left(), dest);
        generateBinOp(op, dest);
        break;

      case EQEQ:
      case NEQ:
      case GT:
      case GEQ:
      case LT:
      case LEQ:
        Register tempReg = resolver.allocate(VarType.DOUBLE);
        // re-resolve left in case it was spilled
        ResolvedOperand left = resolver.resolveFully(op.left());
        resolver.mov(left, tempReg);
        String destName = dest.name();
        ResolvedOperand right = resolver.resolveFully(op.right());
        emitter.emit("comisd %s, %s", tempReg.name(), right.name());
        emitter.emit("%s %s  ; double compare %s", BINARY_OPCODE.get(operator), destName, operator);
        resolver.deallocate(tempReg);
        break;

      default:
        fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
        break;
    }
  }

  private void generateBinOp(BinOp op, ResolvedOperand dest) {
    TokenType operator = op.operator();
    if (!dest.isRegister()) {
      Register tempReg = resolver.allocate(VarType.DOUBLE);
      emitter.emit("; allocated temp reg %s", tempReg);
      resolver.mov(dest, tempReg);
      // Resolve it now because it might have been spilled
      String rightName = resolver.resolve(op.right());
      emitter.emit(
          "%s %s, %s ; double %s",
          BINARY_OPCODE.get(operator),
          tempReg.name(),
          rightName,
          operator);
      resolver.mov(tempReg, dest);
      resolver.deallocate(tempReg);
      // NOTE RETURN
      return;
    }
    String rightName = resolver.resolve(op.right());
    String destName = dest.name();
    emitter.emit(
        "%s %s, %s ; double %s",
        BINARY_OPCODE.get(operator),
        destName,
        rightName,
        operator);
    // nasmCodeGenerator does the deallocs
  }

  @Override
  public void visit(UnaryOp op) {
    VarType leftType = op.operand().type();
    if (leftType != VarType.DOUBLE) {
      return;
    }
    if (op.operator() == TokenType.MINUS) {
      Location destination = op.destination();
      Operand source = op.operand();
      String sourceName = resolver.resolve(source);
      if (resolver.isInAnyRegister(destination) && !source.equals(destination)) {
        Register destReg = resolver.toRegister(destination);
        emitter.emit("xorpd %s, %s", destReg, destReg);
        emitter.emit("subsd %s, %s", destReg, sourceName);
        return;
      }
      Register tempReg = resolver.allocate(VarType.DOUBLE);
      emitter.emit("xorpd %s, %s", tempReg, tempReg);
      // re-resolve it in case it was spilled
      sourceName = resolver.resolve(source);
      emitter.emit("subsd %s, %s", tempReg, sourceName);
      resolver.mov(tempReg, destination);
      resolver.deallocate(tempReg);
    } else {
      fail(op.position(), "Cannot generate %s on doubles", op.operator());
    }
  }
}
