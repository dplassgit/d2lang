package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;

/** Assertions for optimizer tests. */
public class OpcodeSubject extends Subject {

  private final Op actual;

  private OpcodeSubject(FailureMetadata metadata, Op actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  public void isTransferredFrom(Operand source) {
    check("transfer").that(actual).isInstanceOf(Transfer.class);
    Transfer transfer = (Transfer) actual;
    check("isTransferredFrom").that(transfer.source()).isEqualTo(source);
  }

  public void isNop() {
    check("nop").that(actual).isInstanceOf(Nop.class);
  }

  public void isGoto(String label) {
    check("goto").that(actual).isInstanceOf(Goto.class);
    Goto gotop = (Goto) actual;
    check("label").that(gotop.label()).isEqualTo(label);
  }

  public void isBinOp(Location dest, Operand left, TokenType op,
      Operand right) {
    Truth.assertThat(actual).isInstanceOf(BinOp.class);
    BinOp binOp = (BinOp) actual;
    check("isBinOp").that(binOp.destination()).isEqualTo(dest);
    check("isBinOp").that(binOp.left()).isEqualTo(left);
    check("isBinOp").that(binOp.operator()).isEqualTo(op);
    check("isBinOp").that(binOp.right()).isEqualTo(right);
  }

  public void isUnaryOp(Location dest, TokenType op,
      Operand operand) {
    Truth.assertThat(actual).isInstanceOf(UnaryOp.class);
    UnaryOp binOp = (UnaryOp) actual;
    check("isUnaryOp").that(binOp.destination()).isEqualTo(dest);
    check("isUnaryOp").that(binOp.operator()).isEqualTo(op);
    check("isUnaryOp").that(binOp.operand()).isEqualTo(operand);
  }

  public void isExit() {
    Truth.assertThat(actual).isInstanceOf(SysCall.class);
    SysCall syscall = (SysCall) actual;
    check("isExit").that(syscall.call()).isAnyOf(SysCall.Call.MESSAGE,
        SysCall.Call.PARAMETERIZED_MESSAGE);
  }

  public void isReturning(Operand operand) {
    Truth.assertThat(actual).isInstanceOf(Return.class);
    Return returnOp = (Return) actual;
    check("isReturning").that(returnOp.returnValueLocation().get()).isEqualTo(operand);
  }

  public static OpcodeSubject assertThat(Op actual) {
    return assertAbout(OpcodeSubject::new).that(actual);
  }

  public void isInc(Location target) {
    Truth.assertThat(actual).isInstanceOf(Inc.class);
    Inc incOp = (Inc) actual;
    check("isInc").that(incOp.target()).isEqualTo(target);
  }

  public void isDec(Location target) {
    Truth.assertThat(actual).isInstanceOf(Dec.class);
    Dec decOp = (Dec) actual;
    check("isDec").that(decOp.target()).isEqualTo(target);
  }
}
