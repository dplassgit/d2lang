package com.plasstech.lang.d2.codegen;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class DoubleGenerator {
  private static final Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.PLUS, "addsd")
          .put(TokenType.MINUS, "subsd")
          .put(TokenType.MULT, "mulsd")
          .put(TokenType.DIV, "divsd")
          // double comparisons are weird.
          //  .put(TokenType.EQEQ, "setz")
          //  .put(TokenType.NEQ, "setnz")
          //  .put(TokenType.GT, "setg")
          //  .put(TokenType.GEQ, "setge")
          //  .put(TokenType.LT, "setl")
          //  .put(TokenType.LEQ, "setle")
          .build();

  private static final String DIV_BY_ZERO_ERR =
      "DIV_BY_ZERO_ERR: db \"Arithmentic error at line %d: Division by 0\", 10, 0";

  private final Resolver resolver;
  private final Emitter emitter;

  public DoubleGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  public void generate(BinOp op) {
    TokenType operator = op.operator();
    String leftName = resolver.resolve(op.left());
    VarType leftType = op.left().type();
    String rightName = resolver.resolve(op.right());
    String destName = resolver.resolve(op.destination());

    switch (operator) {
      case PLUS:
      case MINUS:
      case MULT:
      case DIV:
        emitter.emit("movsd %s, %s ; double setup", destName, leftName);
        if (operator == TokenType.DIV) {
          // TODO: div by 0 check
        }
        emitter.emit(
            "%s %s, %s ; double %s", BINARY_OPCODE.get(operator), destName, rightName, operator);
        break;

      case EQEQ:
      case NEQ:
      case GT:
      case GEQ:
      case LT:
      case LEQ:
        emitter.fail("Cannot do %s on %ss (yet?)", operator, leftType);
        Register tempReg = resolver.allocate(VarType.DOUBLE);
        emitter.emit("mov %s, %s ; double  compare setup", tempReg.name(), leftName);
        emitter.emit("cmp %s, %s", tempReg.name(), rightName);
        emitter.emit("%s %s  ; double compare %s", BINARY_OPCODE.get(operator), destName, operator);
        resolver.deallocate(tempReg);
        break;

      default:
        emitter.fail("Cannot do %s on %ss (yet?)", operator, leftType);
        break;
    }
  }
}
