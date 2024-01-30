package com.plasstech.lang.d2.codegen.t100;

import java.math.BigDecimal;
import java.math.MathContext;

class Doubles {
  static int[] toT100Format(double in) {
    int[] bytes = new int[8];
    if (in == 0.0) {
      return bytes; // shrug
    }
    boolean neg = in < 0;
    if (neg) {
      in = -in;
    }
    int exponent = (int) (Math.floor(Math.log10(in))) + 65;
    if (neg) {
      exponent += 128;
    }
    bytes[0] = exponent;

    // Remove dot.
    BigDecimal bd = BigDecimal.valueOf(in).round(MathContext.DECIMAL64).stripTrailingZeros();
    String value = bd.toPlainString();
    value = value.replaceAll("\\.", "");
    // Strip leading zeros
    while (value.startsWith("0")) {
      value = value.substring(1);
    }

    boolean hasLeft = false;
    for (int i = 0, j = 1; i < value.length() && j < 8; i++) {
      int digit = value.charAt(i) - '0';
      if (hasLeft) {
        // we're the right half of this digit.
        bytes[j] += digit;
        hasLeft = false;
        j++;
      } else {
        bytes[j] = digit << 4;
        hasLeft = true;
      }
    }
    return bytes;
  }

  private Doubles() {}
}
