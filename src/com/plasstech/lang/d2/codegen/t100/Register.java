package com.plasstech.lang.d2.codegen.t100;

enum Register {
  A,
  B,
  C,
  D,
  E,
  H,
  L,
  BC(B, C, "B", "ldax B", "stax B"),
  DE(D, E, "D", "ldax D", "stax D"),
  M(H, L, "H", "mov A, M", "mov M, A");

  private final Register left;
  private final Register right;
  final String alias;
  final String lda;
  final String sta;

  Register(Register left, Register right, String alias, String lda, String sta) {
    this.left = left;
    this.right = right;
    this.alias = alias;
    this.lda = lda;
    this.sta = sta;
  }

  Register() {
    left = this;
    right = this;
    this.alias = name();
    this.lda = String.format("mov A, %s", name());
    this.sta = String.format("mov %s, A", name());
  }

  Register left() {
    return left;
  }

  Register right() {
    return right;
  }

  boolean isPair() {
    return left != right;
  }
}
