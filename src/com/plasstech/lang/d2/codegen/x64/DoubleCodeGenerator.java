package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
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
    TokenType operator = op.operator();
    String leftName = resolver.resolve(op.left());
    String rightName = resolver.resolve(op.right());
    String destName = resolver.resolve(op.destination());

    switch (operator) {
      case PLUS:
      case MINUS:
      case MULT:
      case DIV:
        if (!destName.equals(leftName)) {
          emitter.emit("movsd %s, %s ; double setup", destName, leftName);
        }
        if (operator == TokenType.DIV) {
          generateDivByZero(op);
        }
        // This fails if dest is not a register?
        // 
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
        fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
        break;
    }
  }

  private void generateDivByZero(BinOp op) {
    Operand rightOperand = op.right();
    if (rightOperand.isConstant()) {
      ConstantOperand<Double> rightConstOperand = (ConstantOperand<Double>) rightOperand;
      double rightValue = rightConstOperand.value();
      if (rightValue == 0) {
        fail("Arithmetic", op.position(), "Division by 0");
      }
    } else {
      Register zeroReg = resolver.allocate(VarType.DOUBLE);
      emitter.emit("xorpd %s, %s  ; instead of mov reg, 0", zeroReg, zeroReg);
      String right = resolver.resolve(rightOperand);
      // NOTE: register needs to be on the left.
      emitter.emit("comisd %s, %s  ; detect division by zero", zeroReg, right);
      resolver.deallocate(zeroReg);
      String continueLabel = Labels.nextLabel("not_div_by_zero");
      emitter.emit("jne %s", continueLabel);

      emitter.emit0("\n  ; division by zero. print error and stop");
      emitter.addData(Messages.DIV_BY_ZERO_ERR);
      emitter.emit("mov EDX, %d  ; line number", op.position().line());
      emitter.emit("mov RCX, DIV_BY_ZERO_ERR");
      emitter.emitExternCall("printf");
      emitter.emitExit(-1);

      emitter.emitLabel(continueLabel);
    }
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
      if (resolver.isInAnyRegister(destination)) {
        Register destReg = resolver.toRegister(destination);
        emitter.emit("xorpd %s, %s  ; instead of mov reg, 0", destReg, destReg);
        emitter.emit("subsd %s, %s", destReg, sourceName);
      } else {
        Register tempReg = resolver.allocate(VarType.DOUBLE);
        emitter.emit("xorpd %s, %s  ; instead of mov reg, 0", tempReg, tempReg);
        emitter.emit("subsd %s, %s", tempReg, sourceName);
        resolver.mov(tempReg, destination);
        resolver.deallocate(tempReg);
      }
    } else {
      fail(op.position(), "Cannot generate %s on doubles", op.operator());
    }
  }
}
