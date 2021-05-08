package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.plasstech.lang.d2.codegen.il.Assignment;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Load;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Store;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.lex.Token.Type;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BoolNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProcedureNode;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.parse.SimpleNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.parse.WhileNode;
import com.plasstech.lang.d2.type.SymTab;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private final ProgramNode root;
  private final SymTab symTab;

  private final List<Op> operations = new ArrayList<>();
  private final Registers registers = new Registers();
  private final List<String> whileStarts = new ArrayList<>();
  private final List<String> whileEnds = new ArrayList<>();

  private int labelId;

  public ILCodeGenerator(ProgramNode root, SymTab symTab) {
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

  private String generateLabel(String prefix) {
    labelId++;
    return String.format("%s%d", prefix, labelId);
  }

  @Override
  public void visit(AssignmentNode node) {
    System.out.printf("\n; %s\n", node);

    String name = node.variable().name();
    Node expr = node.expr();
    expr.accept(this);
    emit(new Store(name, "r0"));
  }

  @Override
  public void visit(IntNode node) {
    // Provide constant in r0
    emit(new Assignment("r0", String.valueOf(node.value())));
  }

  @Override
  public void visit(BoolNode node) {
    // Provide constant in r0
    emit(new Assignment("r0", String.valueOf(node.value())));
  }

  @Override
  public void visit(VariableNode node) {
    // Retrieve location of variable and provide it in r0
    emit(new Load("r0", String.valueOf(node.name())));
  }

  @Override
  public void visit(BinOpNode node) {
    System.out.printf("\n; %s\n", node);

    // Calculate the value and set it in r0
    Node left = node.left();
    // Source for the value of left - either a register or memory location or a
    // value.
    String leftSrc;
    // Possible register
    int leftReg = -1;
    // if left and right are "simple", just get it.
    if (left.isSimpleType()) {
      SimpleNode simpleLeft = (SimpleNode) left;
      leftSrc = simpleLeft.simpleValue();
    } else {
      leftReg = registers.allocate();
      left.accept(this);
      // by definition, "left" has emitted its value in r0. store in rx
      leftSrc = "r" + leftReg;
      emit(new Assignment(leftSrc, "r0"));
    }

    // Calculate the value and set it in r0
    Node right = node.right();
    // Source for the value of right - either a register or memory location or a
    // value.
    String rightSrc;
    // Possible register
    int rightReg = -1;
    // if left and right are "simple", just get it.
    if (right.isSimpleType()) {
      SimpleNode simpleRight = (SimpleNode) right;
      rightSrc = simpleRight.simpleValue();
    } else {
      rightReg = registers.allocate();
      right.accept(this);
      // by definition, "left" has emitted its value in r0. store in rx
      rightSrc = "r" + rightReg;
      emit(new Assignment(rightSrc, "r0"));
    }

//    switch (node.operator()) {
//      case MINUS:
//      case DIV:
//      case MULT:
//      case PLUS:
//      case EQEQ:
    emit(new BinOp("r0", leftSrc, node.operator(), rightSrc));
//        break;
//      default:
//        emit("UNKNOWN OP " + node.operator());
//        break;
//    }

    // now we can deallocate registers
    if (leftReg != -1) {
      registers.deallocate(leftReg);
    }
    if (rightReg != -1) {
      registers.deallocate(rightReg);
    }
  }

  @Override
  public void visit(UnaryNode node) {
    System.out.printf("; %s\n", node);
    // calculate the value and set it in r0
    Node expr = node.expr();
    expr.accept(this);

    switch (node.operator()) {
      case NOT:
      case MINUS: // want r0 to be 0-r0
        emit(new UnaryOp("r0", node.operator(), "r0"));
        break;
      case PLUS:
        // Intentionally do nothing.
        break;
      default:
        break;
    }
  }

  @Override
  public void visit(IfNode node) {
    System.out.printf("\n; %s\n", node);
    String after = generateLabel("afterIf");

    for (IfNode.Case ifCase : node.cases()) {
      Node cond = ifCase.condition();
      cond.accept(this);
      // This is very inefficient, but understandable.
      String thisLabel = generateLabel("then");
      String nextLabel = generateLabel("elif");
      // if it's true, jump to this block
      emit(new IfOp("r0", thisLabel));
      // else, jump to the next one
      emit(new Goto(nextLabel));
      emit(new Label(thisLabel));
      ifCase.block().statements().forEach(stmt -> stmt.accept(this));
      // We're in a block , now jump completely after.
      emit(new Goto(after));
      emit(new Label(nextLabel));
    }
    if (node.elseBlock() != null) {
      System.out.printf("\n; else:");
      node.elseBlock().statements().forEach(stmt -> stmt.accept(this));
    }

    emit(new Label(after));
  }
  
  @Override
  public void visit(WhileNode node) {
    System.out.printf("\n; %s\n", node);

    String before = generateLabel("beforeWhile");
    String after = generateLabel("afterWhile");

    // push before and after on the stacks, for possible use by the "breka" and
    // "continue" nodes.
    emit(new Label(before));
    node.condition().accept(this);
    emit(new UnaryOp("r0", Type.NOT, "r0"));
    emit(new IfOp("r0", after));
    node.block().accept(this);
    if (node.assignment().isPresent()) {
      node.assignment().get().accept(this);
    }
    emit(new Goto(before));
    emit(new Label(after));
  }

  @Override
  public void visit(MainNode node) {
    emit(new Label("_main"));
    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments
    if (node.statements() != null) {
      node.statements().accept(this);
    }
    emit(new Stop());
  }

  @Override
  public void visit(ProcedureNode node) {
    // Guard to prevent just falling into this method
    String afterLabel = generateLabel("afterProc");
    emit(new Goto(afterLabel));

    // note different mangling
    emit(new Label("d_" + node.name()));
    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments
    if (node.statements() != null) {
      node.statements().accept(this);
    }
    emit(new Return());

    emit(new Label(afterLabel));
  }

  private void emit(Op op) {
    System.out.println(op);
    operations.add(op);
  }
}
