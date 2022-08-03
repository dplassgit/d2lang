package com.plasstech.lang.d2.codegen;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class DoubleGenerator {
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

  private static final String ZERO_DBL_NAME = "ZERO_DBL";
  private static final String ZERO_DATA = new DoubleEntry(ZERO_DBL_NAME, 0.0).dataEntry();

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
          generateDivByZero(op);
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
        Register tempReg = resolver.allocate(VarType.DOUBLE);
        emitter.emit("movsd %s, %s ; double compare setup", tempReg.name(), leftName);
        emitter.emit("comisd %s, %s", tempReg.name(), rightName);
        emitter.emit("%s %s  ; double compare %s", BINARY_OPCODE.get(operator), destName, operator);
        resolver.deallocate(tempReg);
        break;

      default:
        emitter.fail("Cannot do %s on %ss (yet?)", operator, leftType);
        break;
    }
  }

  private void generateDivByZero(BinOp op) {
    Operand rightOperand = op.right();
    if (rightOperand.isConstant()) {
      ConstantOperand<Double> rightConstOperand = (ConstantOperand<Double>) rightOperand;
      double rightValue = rightConstOperand.value();
      if (rightValue == 0) {
        throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
      }
    } else {
      Register zeroReg = resolver.allocate(VarType.DOUBLE);
      emitter.addData(ZERO_DATA);
      emitter.emit("movsd %s, [%s]", zeroReg, ZERO_DBL_NAME);
      String right = resolver.resolve(rightOperand);
      // NOTE: register needs to be on the left.
      emitter.emit("comisd %s, %s  ; detect division by zero", zeroReg, right);
      resolver.deallocate(zeroReg);
      String continueLabel = resolver.nextLabel("not_div_by_zero");
      emitter.emit("jne _%s", continueLabel);

      emitter.emit0("\n  ; division by zero. print error and stop");
      emitter.addData(Messages.DIV_BY_ZERO_ERR);
      emitter.emit("mov EDX, %d  ; line number", op.position().line());
      emitter.emit("mov RCX, DIV_BY_ZERO_ERR");
      emitter.emitExternCall("printf");
      emitter.emitExit(-1);

      emitter.emitLabel(continueLabel);
    }
  }
}
