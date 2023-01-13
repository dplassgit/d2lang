package com.plasstech.lang.d2.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.devtools.common.options.OptionsParser;
import com.google.devtools.common.options.OptionsParsingException;

public class D2OptionsTest {
  OptionsParser optionsParser = OptionsParser.newOptionsParser(D2Options.class);

  @Test
  public void noFlags() throws Exception {
    optionsParser.parse(ImmutableList.of("filename"));
    D2Options options = optionsParser.getOptions(D2Options.class);
    assertThat(options.target).isEqualTo(Target.x64);
  }

  @Test
  public void targetFlag() throws Exception {
    optionsParser.parse(ImmutableList.of("--target=t100"));
    D2Options options = optionsParser.getOptions(D2Options.class);
    assertThat(options.target).isEqualTo(Target.t100);
  }

  @Test
  public void badTargetFlag() throws Exception {
    assertThrows(
        OptionsParsingException.class, () -> optionsParser.parse(ImmutableList.of("--target=xyz")));
  }
}
