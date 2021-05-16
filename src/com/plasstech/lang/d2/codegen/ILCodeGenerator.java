package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.plasstech.lang.d2.codegen.il.Assignment;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Goto;
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
import com.plasstech.lang.d2.parse.BreakNode;
import com.plasstech.lang.d2.parse.ConstNode;
import com.plasstech.lang.d2.parse.ContinueNode;
import com.plasstech.lang.d2.parse.IfNode;
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
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private final ProgramNode root;
  private final SymTab symTab;

  private final List<Op> operations = new ArrayList<>();
  private final Stack<String> whileBreaks = new Stack<>();
  private final Stack<String> whileContinues = new Stack<>();

  private int tempId;
  private int labelId;
  private Symbol t0 = new Symbol("t0", SymbolStorage.GLOBAL);

  public ILCodeGenerator(ProgramNode root, SymTab symTab) {
    this.root = root;
    this.symTab = symTab;
    // This is potentially wrong.
    t0.setType(VarType.INT);
  }

  @Override
  public List<Op> generate() {
    System.out.println("#include <stdlib.h>");
    System.out.println("#include <stdio.h>");
    System.out.println("main() {");
    if (symTab != null) {
      for (Symbol symbol : symTab.entries().values()) {
        emitTypeDeclaration(symbol);
      }
    }
    emitTypeDeclaration(t0);
    root.accept(this);
    System.out.println("}");
    return operations;
  }

  private void emitTypeDeclaration(Symbol symbol) {
    switch (symbol.type()) {
      case INT:
      case BOOL:
        System.out.printf("int %s;\n", symbol.name());
        break;
      case STRING:
        System.out.printf("char *%s;\n", symbol.name());
        break;
      default:
        System.out.printf("void *%s;\n", symbol.name());
        break;
    }
  }

  @Override
  public void visit(PrintNode node) {
    System.out.printf("\n// %s\n", node);
    Node expr = node.expr();
    expr.accept(this);
    emit(new SysCall("$ffd2", "t0"));
//    System.out.println("\tprintf(\"%d\\n\", t0);");
  }

  private String generateLabel(String prefix) {
    labelId++;
    return String.format("%s%d", prefix, labelId);
  }

  private String generateTemp(VarType varType) {
    tempId++;
    switch (varType) {
      case BOOL:
      case INT:
        System.out.printf("int t%d;\n", tempId);
        break;
      case STRING:
        System.out.printf("char *t%d;\n", tempId);
        break;
      default:
        System.out.printf("void *t%d;\n", tempId);
        break;
    }
    return String.format("t%d", tempId);
  }

  @Override
  public void visit(AssignmentNode node) {
//    System.out.printf("\n// %s\n", node);

    String name = node.variable().name();
    Node expr = node.expr();
    expr.accept(this);
    emit(new Store(name, "t0"));
  }

  @Override
  public <T> void visit(ConstNode<T> node) {
    // Provide constant in t0
    if (node.nodeType() == Node.Type.STRING) {
      // TODO: figure out storage for strings
      emit(new Assignment("t0", String.format("\"%s\"", node.simpleValue())));
    } else {
      emit(new Assignment("t0", node.simpleValue()));
    }
  }

  @Override
  public void visit(VariableNode node) {
    // Retrieve location of variable and provide it in t0
    emit(new Load("t0", node.name()));
  }

  @Override
  public void visit(BinOpNode node) {
    System.out.printf("\n// %s\n", node);

    // Calculate the value and set it in t0
    Node left = node.left();
    // Source for the value of left - either a register or memory location or a value.
    String leftSrc;
    // if left and right are "simple", just get it.
    if (left.isSimpleType()) {
      SimpleNode simpleLeft = (SimpleNode) left;
      leftSrc = simpleLeft.simpleValue();
    } else {
      leftSrc = generateTemp(left.varType());
      left.accept(this);
      // by definition, "left" has emitted its value in t0. Copy to "leftSrc"
      emit(new Assignment(leftSrc, "t0"));
    }

    // Calculate the value and set it in t0
    Node right = node.right();
    // Source for the value of right - either a register or memory location or a
    // value.
    String rightSrc;
    // if left and right are "simple", just get it.
    if (right.isSimpleType()) {
      SimpleNode simpleRight = (SimpleNode) right;
      rightSrc = simpleRight.simpleValue();
    } else {
      rightSrc = generateTemp(left.varType());
      right.accept(this);
      // by definition, "right" has emitted its value in t0. Copy to "rightSrc"
      emit(new Assignment(rightSrc, "t0"));
    }

//    switch (node.operator()) {
//      case MINUS:
//      case DIV:
//      case MULT:
//      case PLUS:
//      case EQEQ:
    emit(new BinOp("t0", leftSrc, node.operator(), rightSrc));
//        break;
//      default:
//        emit("UNKNOWN OP " + node.operator());
//        break;
//    }

  }

  @Override
  public void visit(UnaryNode node) {
    System.out.printf("\n// %s\n", node);
    // calculate the value and set it in t0
    Node expr = node.expr();
    expr.accept(this);

    switch (node.operator()) {
      case NOT:
      case MINUS: // want t0 to be 0-t0
        emit(new UnaryOp("t0", node.operator(), "t0"));
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
    System.out.printf("\n// %s\n", node);
    String after = generateLabel("afterIf");

    for (IfNode.Case ifCase : node.cases()) {
      String nextLabel = generateLabel("elif");

      Node cond = ifCase.condition();
      cond.accept(this);
      // if it's not true, jump to the next block
      emit(new UnaryOp("t0", Type.NOT, "t0"));
      emit(new IfOp("t0", nextLabel));

      ifCase.block().statements().forEach(stmt -> stmt.accept(this));
      // We're in a block , now jump completely after.
      emit(new Goto(after));

      emit(new Label(nextLabel));
    }
    if (node.elseBlock() != null) {
      System.out.printf("\n// else:");
      node.elseBlock().statements().forEach(stmt -> stmt.accept(this));
    }

    emit(new Label(after));
  }

  @Override
  public void visit(WhileNode node) {
    System.out.printf("\n// %s\n", node);

    // before:
    // ..test
    // ..if done, goto after
    // ..(loop code)
    // increment: ("continue" target)
    // ..(increment code)
    // ..goto before
    // after: ("break" target)
    String before = generateLabel("beforeWhile");
    String increment = generateLabel("increment");
    String after = generateLabel("afterWhile");
    whileContinues.push(increment);
    whileBreaks.push(after);

    System.out.println("// while");
    emit(new Label(before));
    node.condition().accept(this);
    emit(new UnaryOp("t0", Type.NOT, "t0"));
    emit(new IfOp("t0", after));

    System.out.println("// do");
    node.block().accept(this);

    emit(new Label(increment));
    if (node.assignment().isPresent()) {
      node.assignment().get().accept(this);
    }
    emit(new Goto(before));

    emit(new Label(after));
    whileBreaks.pop();
    whileContinues.pop();
  }

  @Override
  public void visit(BreakNode node) {
    // break = go to the end
    System.out.println("// break");
    String after = whileBreaks.peek();
    emit(new Goto(after));
  }

  @Override
  public void visit(ContinueNode node) {
    System.out.println("// continue");
    // continue = go to the "increment"
    String before = whileContinues.peek();
    emit(new Goto(before));
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
