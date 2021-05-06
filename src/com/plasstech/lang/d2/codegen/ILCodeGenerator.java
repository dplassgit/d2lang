package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.plasstech.lang.d2.codegen.il.Assignment;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BoolNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.type.SymTab;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private final StatementsNode root;
  private final SymTab symTab;
  private List<Op> operations = new ArrayList<>();
  private final Registers registers = new Registers();

  public ILCodeGenerator(StatementsNode root, SymTab symTab) {
    this.root = root;
    this.symTab = symTab;
  }

  @Override
  public List<Op> generate() {
    root.accept(this);
    return operations;
  }

  @Override
  public void visit(PrintNode node) {
    System.out.printf("\n; %s\n", node);
    Node expr = node.expr();
    expr.accept(this);
    emit(new SysCall("$ffd2", "r0"));
  }

  @Override
  public void visit(AssignmentNode node) {
    System.out.printf("\n; %s\n", node);
    String name = node.variable().name();
    Node expr = node.expr();
    expr.accept(this);
    emit(new Assignment(name, "r0"));
  }

  @Override
  public void visit(IntNode node) {
    System.out.printf("; %s\n", node);
    // Provide constant in r0
    emit(new Assignment("r0", String.valueOf(node.value())));
  }

  @Override
  public void visit(BoolNode node) {
    System.out.printf("; %s\n", node);
    // Provide constant in r0
    emit(new Assignment("r0", String.valueOf(node.value())));
  }

  @Override
  public void visit(VariableNode node) {
    System.out.printf("; %s\n", node);
    // Retrieve location of variable and provide it in r0
    emit(new Assignment("r0", String.valueOf(node.name())));
  }

  @Override
  public void visit(BinOpNode node) {
    System.out.printf("\n; %s\n", node);
    // calculate the value and set it in r0
    Node left = node.left();

    // destination for the calculation
    int leftReg = registers.allocate();
    left.accept(this);
    // by definition, "left" has emitted its value in r0. store in rx
    emit(new Assignment("r" + leftReg, "r0"));
//    String leftVal;
//    // if left and right are "simple", just get it.
//    if (left.isSimpleType()) {
//      SimpleNode simpleLeft = (SimpleNode) left;
//      leftVal = simpleLeft.simpleValue();
//    } else {
//      left.accept(this);
//      // by definition, "left" has emitted its value in r0. store in rx
//      leftVal = String.format("r%d", dest);
//      emit(new Assignment(leftVal, "r0"));
//    }

    Node right = node.right();
    int rightReg = registers.allocate();
    right.accept(this);
    // by definition, "left" has emitted its value in r0. store in rx
    emit(new Assignment("r" + rightReg, "r0"));

//    switch (node.operator()) {
//      case MINUS:
//      case DIV:
//      case MULT:
//      case PLUS:
//      case EQEQ:
        emit(new BinOp("r0", "r" + leftReg, node.operator(), "r" + rightReg));
//        break;
//      default:
//        emit("UNKNOWN OP " + node.operator());
//        break;
//    }

    // now we can deallocate registers
    registers.deallocate(leftReg);
    registers.deallocate(rightReg);
  }

  @Override
  public void visit(UnaryNode node) {
    System.out.printf("; %s\n", node);
    // calculate the value and set it in r0
    Node expr = node.expr();
    expr.accept(this);

    switch (node.operator()) {
      case NOT:
      case MINUS: // want r0 to be 0-r1
        emit(new UnaryOp("r0", node.operator(), "r0"));

//        emit("ld r1, r0");
//        emit("ld r0, #0");
//        emit("sbc");
//        emit("sub r0, r1\n");
        break;
      case PLUS:
        // Intentionally do nothing.
        break;
      default:
        break;
    }
  }

  private void emit(String ignored) {
  }

  private void emit(Op op) {
    System.err.println(op);
    operations.add(op);
  }
}
