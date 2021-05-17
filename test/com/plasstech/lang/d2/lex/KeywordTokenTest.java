package com.plasstech.lang.d2.lex;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;

public class KeywordTokenTest {

  @Test
  public void constructor_passes() {
    for (KeywordType kt : KeywordType.values()) {
      if (kt != KeywordType.TRUE && kt != KeywordType.FALSE) {
        new KeywordToken(null, null, kt);
      }
    }
  }

  @Test
  public void constructor_rejectsTrueFalse() {
    assertThrows(IllegalArgumentException.class,
            () -> new KeywordToken(null, null, KeywordType.TRUE));
    assertThrows(IllegalArgumentException.class,
            () -> new KeywordToken(null, null, KeywordType.FALSE));
  }
}
